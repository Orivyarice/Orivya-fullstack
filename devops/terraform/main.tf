# ══════════════════════════════════════════════════
# main.tf — Orivya Rice AWS Infrastructure
# Creates: VPC, EC2, RDS MySQL, S3, ECR
#
# Prerequisites:
# 1. Install Terraform: https://terraform.io
# 2. Install AWS CLI: aws configure
# 3. Run: terraform init → terraform plan → terraform apply
# ══════════════════════════════════════════════════

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  required_version = ">= 1.5.0"

  # Store state in S3 (uncomment after creating the bucket manually)
  # backend "s3" {
  #   bucket = "orivya-terraform-state"
  #   key    = "prod/terraform.tfstate"
  #   region = "ap-south-1"
  # }
}

provider "aws" {
  region = var.aws_region
}

# ── Variables ─────────────────────────────────────
variable "aws_region"    { default = "ap-south-1" }   # Mumbai region
variable "app_name"      { default = "orivya-rice" }
variable "db_password"   { sensitive = true }
variable "instance_type" { default = "t3.micro" }     # Free tier eligible

# ── VPC ───────────────────────────────────────────
resource "aws_vpc" "orivya_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags = { Name = "${var.app_name}-vpc" }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.orivya_vpc.id
  tags   = { Name = "${var.app_name}-igw" }
}

resource "aws_subnet" "public_1" {
  vpc_id                  = aws_vpc.orivya_vpc.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.aws_region}a"
  map_public_ip_on_launch = true
  tags = { Name = "${var.app_name}-public-1" }
}

resource "aws_subnet" "public_2" {
  vpc_id                  = aws_vpc.orivya_vpc.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.aws_region}b"
  map_public_ip_on_launch = true
  tags = { Name = "${var.app_name}-public-2" }
}

resource "aws_route_table" "public_rt" {
  vpc_id = aws_vpc.orivya_vpc.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }
  tags = { Name = "${var.app_name}-public-rt" }
}

resource "aws_route_table_association" "public_1" {
  subnet_id      = aws_subnet.public_1.id
  route_table_id = aws_route_table.public_rt.id
}

resource "aws_route_table_association" "public_2" {
  subnet_id      = aws_subnet.public_2.id
  route_table_id = aws_route_table.public_rt.id
}

# ── Security Groups ────────────────────────────────
resource "aws_security_group" "app_sg" {
  name        = "${var.app_name}-app-sg"
  description = "Security group for Orivya app"
  vpc_id      = aws_vpc.orivya_vpc.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP"
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS"
  }

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Spring Boot"
  }

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]   # Restrict to your IP in production
    description = "SSH"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.app_name}-app-sg" }
}

resource "aws_security_group" "db_sg" {
  name        = "${var.app_name}-db-sg"
  description = "Security group for RDS MySQL"
  vpc_id      = aws_vpc.orivya_vpc.id

  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.app_sg.id]
    description     = "MySQL from app only"
  }

  tags = { Name = "${var.app_name}-db-sg" }
}

# ── EC2 Instance ───────────────────────────────────
resource "aws_key_pair" "orivya_key" {
  key_name   = "${var.app_name}-key"
  public_key = file("~/.ssh/id_rsa.pub")   # Your SSH public key
}

resource "aws_instance" "app_server" {
  ami                    = "ami-0f58b397bc5c1f2e8"  # Ubuntu 22.04 ap-south-1
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public_1.id
  vpc_security_group_ids = [aws_security_group.app_sg.id]
  key_name               = aws_key_pair.orivya_key.key_name

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  user_data = <<-EOF
    #!/bin/bash
    apt-get update -y
    apt-get install -y docker.io docker-compose-v2 git
    systemctl start docker
    systemctl enable docker
    usermod -aG docker ubuntu

    # Clone your repo (replace with your git repo URL)
    git clone https://github.com/yourusername/orivya-fullstack.git /app
    cd /app
    docker compose up -d
  EOF

  tags = { Name = "${var.app_name}-server" }
}

# ── RDS MySQL ──────────────────────────────────────
resource "aws_db_subnet_group" "orivya_db_subnet" {
  name       = "${var.app_name}-db-subnet"
  subnet_ids = [aws_subnet.public_1.id, aws_subnet.public_2.id]
}

resource "aws_db_instance" "mysql" {
  identifier           = "${var.app_name}-mysql"
  engine               = "mysql"
  engine_version       = "8.0"
  instance_class       = "db.t3.micro"   # Free tier eligible
  allocated_storage    = 20
  storage_type         = "gp2"

  db_name  = "orivya_db"
  username = "root"
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.orivya_db_subnet.name
  vpc_security_group_ids = [aws_security_group.db_sg.id]

  skip_final_snapshot = true   # Set to false in production!
  publicly_accessible = false

  tags = { Name = "${var.app_name}-rds" }
}

# ── S3 for product images ─────────────────────────
resource "aws_s3_bucket" "images" {
  bucket = "${var.app_name}-product-images"
  tags   = { Name = "${var.app_name}-images" }
}

resource "aws_s3_bucket_public_access_block" "images" {
  bucket                  = aws_s3_bucket.images.id
  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

# ── ECR for Docker images ─────────────────────────
resource "aws_ecr_repository" "backend" {
  name                 = "${var.app_name}-backend"
  image_tag_mutability = "MUTABLE"
  tags                 = { Name = "${var.app_name}-backend-ecr" }
}

resource "aws_ecr_repository" "frontend" {
  name                 = "${var.app_name}-frontend"
  image_tag_mutability = "MUTABLE"
  tags                 = { Name = "${var.app_name}-frontend-ecr" }
}

# ── Outputs ───────────────────────────────────────
output "app_server_ip" {
  value       = aws_instance.app_server.public_ip
  description = "EC2 public IP — access app at http://<this-ip>"
}

output "rds_endpoint" {
  value       = aws_db_instance.mysql.endpoint
  description = "RDS MySQL endpoint for application.properties"
}

output "ecr_backend_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "ecr_frontend_url" {
  value = aws_ecr_repository.frontend.repository_url
}
