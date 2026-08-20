# Email Campaign System - Comprehensive Implementation Plan
## For ChemOS Platform - Enterprise Grade Solution

**Document Version**: 1.0  
**Target Audience**: Backend Developer  
**Project Duration**: 8-12 weeks (single developer)  
**Last Updated**: 2026-07-31

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [System Requirements & Specifications](#2-system-requirements--specifications)
3. [Architecture Design](#3-architecture-design)
4. [Technology Stack & Rationale](#4-technology-stack--rationale)
5. [Database Design](#5-database-design)
6. [Detailed Implementation Phases](#6-detailed-implementation-phases)
7. [Security Implementation](#7-security-implementation)
8. [Testing Strategy](#8-testing-strategy)
9. [Deployment Strategy](#9-deployment-strategy)
10. [Monitoring & Observability](#10-monitoring--observability)
11. [Performance & Scalability](#11-performance--scalability)
12. [Disaster Recovery & Business Continuity](#12-disaster-recovery--business-continuity)
13. [Cost Analysis](#13-cost-analysis)
14. [Risk Assessment & Mitigation](#14-risk-assessment--mitigation)
15. [Compliance & Legal Considerations](#15-compliance--legal-considerations)
16. [Day-by-Day Implementation Schedule](#16-day-by-day-implementation-schedule)

---

## 1. Executive Summary

### 1.1 Project Overview
Building an enterprise-grade email campaign system for South Asia's largest chemical company to enable targeted marketing campaigns to 10,000+ customers with personalized offers on chemical products.

### 1.2 Core Objectives
- **Reliability**: 99.9% email delivery success rate
- **Security**: Enterprise-grade security with audit trails
- **Scalability**: Handle 10K → 100K → 1M customers
- **Personalization**: Avoid generic emails with smart template rotation
- **Tracking**: Complete visibility into campaign performance
- **Reply Management**: Capture and track customer responses

### 1.3 Success Metrics
- Email delivery rate: >95%
- Inbox placement rate: >80%
- Campaign creation time: <2 minutes
- Email send rate: 500+ emails/minute at peak
- System uptime: 99.9%
- Reply capture rate: 100%

---

## 2. System Requirements & Specifications

### 2.1 Functional Requirements

#### FR-1: Campaign Management
- **FR-1.1**: MD/Sales users can create campaigns via ChemOS portal
- **FR-1.2**: Select chemical products from existing ChemOS product catalog
- **FR-1.3**: Set offer price and discount percentage
- **FR-1.4**: Select recipients from customer database (single, multiple, or filtered list)
- **FR-1.5**: Preview email before sending
- **FR-1.6**: Schedule campaigns for future date/time
- **FR-1.7**: Save campaigns as drafts
- **FR-1.8**: Cancel scheduled campaigns
- **FR-1.9**: Clone existing campaigns

#### FR-2: Template Management
- **FR-2.1**: Support minimum 10 unique email templates
- **FR-2.2**: Templates must support variable injection (customer name, company, chemical, price)
- **FR-2.3**: Templates include images, logos, and branded content
- **FR-2.4**: Support HTML email with fallback to plain text
- **FR-2.5**: Template versioning and A/B testing capability
- **FR-2.6**: Admin interface to create/edit templates

#### FR-3: Recipient Management
- **FR-3.1**: Filter customers by chemical purchased
- **FR-3.2**: Filter by customer segment (geography, company size, purchase history)
- **FR-3.3**: Upload CSV for bulk recipient list
- **FR-3.4**: Exclude recipients (unsubscribe list)
- **FR-3.5**: Deduplicate email addresses
- **FR-3.6**: Validate email addresses before sending

#### FR-4: Email Sending
- **FR-4.1**: Send personalized emails to selected recipients
- **FR-4.2**: Smart template rotation (avoid same template to same recipient)
- **FR-4.3**: Throttling to respect ISP limits
- **FR-4.4**: Retry failed emails with exponential backoff
- **FR-4.5**: Track sending status (queued, sent, failed, bounced)
- **FR-4.6**: Real-time progress monitoring during send

#### FR-5: Tracking & Analytics
- **FR-5.1**: Track email opens (with pixel tracking)
- **FR-5.2**: Track link clicks (with redirect tracking)
- **FR-5.3**: Track bounces (hard and soft)
- **FR-5.4**: Track unsubscribes
- **FR-5.5**: Campaign-level analytics dashboard
- **FR-5.6**: Recipient-level delivery status
- **FR-5.7**: Export analytics to CSV/Excel

#### FR-6: Reply Handling
- **FR-6.1**: Capture all replies to campaign emails
- **FR-6.2**: Link replies to original campaign and recipient
- **FR-6.3**: Store full email thread
- **FR-6.4**: Notify sales team of replies
- **FR-6.5**: Mark replies as read/unread
- **FR-6.6**: Search replies by campaign, customer, or keyword

#### FR-7: Compliance & Opt-out
- **FR-7.1**: Include unsubscribe link in every email
- **FR-7.2**: Honor unsubscribe requests immediately
- **FR-7.3**: Maintain global unsubscribe list
- **FR-7.4**: Include physical mailing address (CAN-SPAM compliance)
- **FR-7.5**: Respect Do-Not-Contact lists

### 2.2 Non-Functional Requirements

#### NFR-1: Performance
- Campaign creation response time: <2 seconds
- Email send rate: Minimum 500 emails/minute
- Database query response: <500ms for 95th percentile
- UI load time: <3 seconds
- API response time: <1 second for 95th percentile

#### NFR-2: Scalability
- Support 10,000 customers (current)
- Scale to 100,000 customers within 6 months
- Scale to 1,000,000 customers within 2 years
- Handle 50 concurrent campaigns
- Process 100,000 emails in a single campaign

#### NFR-3: Reliability
- System uptime: 99.9% (43.8 minutes downtime/month acceptable)
- Email delivery success rate: >95%
- Zero data loss for campaigns and replies
- Automatic recovery from failures
- Graceful degradation under load

#### NFR-4: Security
- Encryption at rest (database, S3)
- Encryption in transit (TLS 1.3)
- Role-based access control (RBAC)
- Audit logging for all actions
- Secure credential management (no hardcoded secrets)
- Protection against email spoofing
- Rate limiting to prevent abuse

#### NFR-5: Maintainability
- Clean code with proper documentation
- Automated testing (unit, integration, E2E)
- Automated deployment pipeline
- Comprehensive logging and monitoring
- Easy rollback mechanism
- Code coverage >80%

#### NFR-6: Usability
- Intuitive UI matching ChemOS design
- Clear error messages
- Progress indicators for long operations
- Undo/rollback for critical actions
- Mobile-responsive design

---

## 3. Architecture Design

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         ChemOS Platform                          │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │          Frontend (React Admin Portal)                     │ │
│  │  - Campaign Creation UI                                    │ │
│  │  - Template Management                                     │ │
│  │  - Analytics Dashboard                                     │ │
│  │  - Reply Inbox                                             │ │
│  └──────────────────────┬─────────────────────────────────────┘ │
│                         │ HTTPS                                  │
│  ┌──────────────────────▼─────────────────────────────────────┐ │
│  │       Spring Boot API (Campaign Module)                    │ │
│  │  - Campaign Controller                                     │ │
│  │  - Template Controller                                     │ │
│  │  - Analytics Controller                                    │ │
│  │  - Reply Controller                                        │ │
│  │  - Authentication/Authorization (existing)                 │ │
│  └──────────────────────┬─────────────────────────────────────┘ │
│                         │                                        │
└─────────────────────────┼────────────────────────────────────────┘
                          │
         ┌────────────────┼────────────────┐
         │                │                │
         ▼                ▼                ▼
    ┌─────────┐    ┌──────────┐    ┌──────────┐
    │PostgreSQL│    │   SQS    │    │    S3    │
    │   RDS    │    │  Queues  │    │Templates │
    │          │    │          │    │  Images  │
    └─────────┘    └────┬─────┘    └──────────┘
                        │
                        │ Poll Messages
                        │
         ┌──────────────┴──────────────┐
         │                             │
         ▼                             ▼
    ┌──────────────┐           ┌──────────────┐
    │ Email Worker │           │ Email Worker │
    │ (ECS Fargate)│           │ (ECS Fargate)│
    │              │           │              │
    │ - Template   │ Auto      │ - Template   │
    │   Rendering  │ Scale     │   Rendering  │
    │ - SES Send   │ 1-20      │ - SES Send   │
    │ - DB Update  │ Tasks     │ - DB Update  │
    └──────┬───────┘           └──────┬───────┘
           │                          │
           └──────────┬───────────────┘
                      │
                      ▼
              ┌───────────────┐
              │  Amazon SES   │
              │               │
              │ - Email Send  │
              │ - Bounce/     │
              │   Complaint   │
              │   Handling    │
              └───────┬───────┘
                      │
                      ▼
              ┌───────────────┐
              │   Customer    │
              │   Inbox       │
              │               │
              │   (Reply)     │
              └───────┬───────┘
                      │
                      ▼
              ┌───────────────┐
              │  SES Inbound  │
              │  Rule Set     │
              │               │
              │  S3 + Lambda  │
              └───────┬───────┘
                      │
                      ▼
              ┌───────────────┐
              │   PostgreSQL  │
              │  Replies Table│
              └───────────────┘
```

### 3.2 Component Breakdown

#### 3.2.1 ChemOS Spring Boot API (Campaign Module)
**Responsibility**: Business logic, API endpoints, transaction management

**Components**:
- **CampaignService**: Create, update, schedule, cancel campaigns
- **TemplateService**: Manage email templates
- **RecipientService**: Filter and validate recipients
- **AnalyticsService**: Aggregate campaign metrics
- **ReplyService**: Query and manage replies
- **OutboxPublisher**: Publish events to SQS using Outbox pattern

**Key Design Patterns**:
- **Outbox Pattern**: Transactional event publishing
- **Repository Pattern**: Data access abstraction
- **Service Layer Pattern**: Business logic isolation
- **DTO Pattern**: Data transfer between layers

#### 3.2.2 PostgreSQL Database
**Responsibility**: Persistent storage, ACID transactions, complex queries

**Schemas**:
- **Core Schema**: campaigns, recipients, templates, users
- **Event Schema**: outbox_events, email_jobs, tracking_events
- **Reply Schema**: email_replies, reply_threads
- **Audit Schema**: audit_logs, user_actions

**Optimization**:
- Partitioning on `email_jobs` by created_at (monthly partitions)
- Indexes on frequently queried columns
- Connection pooling (HikariCP)
- Read replicas for analytics queries

#### 3.2.3 Amazon SQS
**Responsibility**: Message queuing, decoupling, retry mechanism

**Queues**:
1. **campaign-email-send.fifo**: Campaign email jobs (FIFO for ordering)
2. **campaign-email-retry**: Failed emails for retry
3. **campaign-reply-process**: Inbound reply processing
4. **campaign-dlq**: Dead letter queue for manual intervention

**Configuration**:
- Visibility timeout: 5 minutes
- Message retention: 14 days
- Max receive count: 3 (then move to DLQ)
- Long polling: 20 seconds

#### 3.2.4 Email Worker (ECS Fargate)
**Responsibility**: Email rendering, sending, status updates

**Workflow**:
1. Poll SQS for email jobs
2. Fetch template from S3
3. Fetch recipient data from DB
4. Render HTML with Thymeleaf
5. Send via SES with retry logic
6. Update email_jobs status in DB
7. Delete message from SQS

**Auto-Scaling**:
- Target: Queue depth < 100 messages
- Min tasks: 1
- Max tasks: 20
- Scale-out threshold: Queue depth > 100 for 1 minute
- Scale-in threshold: Queue depth < 50 for 5 minutes

#### 3.2.5 Amazon SES
**Responsibility**: Email delivery, bounce/complaint handling

**Configuration**:
- Domain verification: campaigns.chemos.com
- DKIM signing: Enabled
- SPF record: v=spf1 include:amazonses.com ~all
- DMARC policy: v=DMARC1; p=quarantine; rua=mailto:dmarc@chemos.com
- Dedicated IP: Optional (for >100K emails/month)
- Sending rate: Start 1 email/sec, scale to 50 emails/sec

#### 3.2.6 S3 Storage
**Responsibility**: Template storage, attachment storage, inbound email storage

**Buckets**:
- `chemos-email-templates`: HTML email templates
- `chemos-email-attachments`: Logos, images, PDFs
- `chemos-email-inbound`: Raw inbound emails from SES
- `chemos-email-backups`: Database backups

**Lifecycle Policies**:
- Inbound emails: Move to Glacier after 90 days
- Backups: Delete after 365 days

#### 3.2.7 Tracking Infrastructure
**Components**:
- **Tracking Pixel Service**: Lambda@Edge for open tracking
- **Link Redirect Service**: Lambda for click tracking
- **Event Aggregator**: Kinesis Firehose → S3 → Athena

### 3.3 Data Flow Diagrams

#### 3.3.1 Campaign Creation Flow
```
User → ChemOS UI → API Controller → CampaignService
                                          ↓
                                    Validate Input
                                          ↓
                                    DB Transaction Start
                                          ↓
                           ┌──────────────┴───────────────┐
                           ↓                              ↓
                    Insert Campaign                Insert Outbox Events
                    Insert Recipients              (for each recipient)
                           ↓                              ↓
                    DB Transaction Commit ←──────────────┘
                           ↓
                    Outbox Publisher (async job)
                           ↓
                    Publish to SQS
                           ↓
                    Return Success to User
```

#### 3.3.2 Email Sending Flow
```
Email Worker → Poll SQS → Receive Message
                               ↓
                         Parse Job Details
                               ↓
                    Fetch Template from S3
                               ↓
                    Fetch Recipient Data (DB)
                               ↓
                    Render HTML (Thymeleaf)
                               ↓
                    Inject Tracking Pixel & Links
                               ↓
                    Send via SES
                               ↓
                    ┌──────────┴─────────┐
                    ↓                    ↓
                Success              Failed
                    ↓                    ↓
            Update Status=SENT    Retry Count++
            Delete SQS Msg              ↓
                                  Retry < 3?
                                  ↓         ↓
                                Yes        No
                                  ↓         ↓
                            Re-queue   Move to DLQ
                                       Status=FAILED
```

#### 3.3.3 Reply Handling Flow
```
Customer Reply → Inbound SES Rule → S3 Storage
                                         ↓
                                    SNS Topic
                                         ↓
                                  Lambda Function
                                         ↓
                              Parse Email Headers
                              Extract Campaign ID
                              Extract From/Subject/Body
                                         ↓
                              Insert into replies table
                                         ↓
                              Publish to SQS (reply queue)
                                         ↓
                              Reply Worker processes
                                         ↓
                              Send notification to Sales
```

---

## 4. Technology Stack & Rationale

### 4.1 Backend Technologies

#### 4.1.1 Spring Boot 3.x (Java 17+)
**Why?**
- Already used in ChemOS - code reuse
- Enterprise-grade framework with proven stability
- Excellent Spring Data JPA for database operations
- Spring Cloud AWS for seamless AWS integration
- Built-in security with Spring Security
- Mature ecosystem and extensive documentation

**Alternatives Considered**:
- Node.js: Async I/O good for email workers but team unfamiliar
- Python Django: Good for rapid dev but less enterprise adoption
- .NET Core: Excellent but requires .NET expertise

**Decision**: Stick with Spring Boot for consistency

#### 4.1.2 PostgreSQL 15+
**Why?**
- ACID compliance for transactional integrity
- JSON/JSONB columns for flexible metadata storage
- Excellent full-text search for email body search
- Partitioning support for large tables
- Mature replication and backup tools
- Cost-effective on AWS RDS

**Alternatives Considered**:
- MySQL: Less robust JSON support
- MongoDB: No ACID for multi-document transactions
- DynamoDB: Great for scale but harder to query

**Decision**: PostgreSQL for reliability and query flexibility

#### 4.1.3 Amazon SQS
**Why?**
- Fully managed - no infrastructure to maintain
- Auto-scaling and high availability built-in
- FIFO queues for ordered processing
- Dead letter queues for error handling
- At-least-once delivery guarantee
- Very low cost ($0.40 per million requests)

**Alternatives Considered**:
- RabbitMQ: More features but requires self-hosting
- Apache Kafka: Overkill for this use case
- Redis Queues: Requires Redis management

**Decision**: SQS for simplicity and AWS integration

#### 4.1.4 Amazon SES
**Why?**
- Cheapest option ($0.10 per 1,000 emails)
- Built-in bounce/complaint handling
- DKIM/SPF support for deliverability
- Native AWS integration
- Scales to millions of emails
- Detailed sending metrics

**Alternatives Considered**:
- SendGrid: $15/month for 40K emails (expensive at scale)
- Mailgun: $35/month for 50K emails
- Postmark: $10/month for 10K emails (limited)

**Decision**: SES for cost and scalability

#### 4.1.5 ECS Fargate
**Why?**
- Serverless container execution - no EC2 management
- Auto-scaling based on queue depth
- Pay per second of execution
- Easy deployment with Docker
- IAM integration for security
- CloudWatch integration for logs

**Alternatives Considered**:
- Lambda: 15-min timeout may be limiting, cold starts
- EC2: Requires server management
- Kubernetes: Overkill for this scale

**Decision**: Fargate for operational simplicity

### 4.2 Frontend Technologies

#### 4.2.1 React 18+ (TypeScript)
**Why?**
- Already used in ChemOS admin portal
- Component reuse from existing codebase
- Rich UI libraries (Material-UI, Ant Design)
- TypeScript for type safety
- Excellent developer experience

#### 4.2.2 Material-UI (MUI)
**Why?**
- Professional-looking components
- Consistent with ChemOS design
- Accessibility built-in
- Responsive by default

### 4.3 DevOps & Infrastructure

#### 4.3.1 Docker
**Why?**
- Consistent environments (dev, staging, prod)
- Easy deployment to ECS Fargate
- Dependency isolation

#### 4.3.2 Terraform (Infrastructure as Code)
**Why?**
- Version control infrastructure
- Reproducible deployments
- Multi-environment support (dev, staging, prod)
- Better than CloudFormation for readability

#### 4.3.3 GitHub Actions (CI/CD)
**Why?**
- Integrated with GitHub
- Free for private repos
- Easy to configure
- Supports Docker builds

#### 4.3.4 CloudWatch (Monitoring)
**Why?**
- Native AWS integration
- Centralized logging
- Custom metrics and alarms
- Dashboards for visualization

---

## 5. Database Design

### 5.1 Schema Overview

```sql
-- Core Schema
campaigns
campaign_recipients
templates
email_jobs
email_tracking_events
email_replies
outbox_events
unsubscribe_list
audit_logs
```

### 5.2 Detailed Table Definitions

#### 5.2.1 campaigns
```sql
CREATE TABLE campaigns (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Product & Offer
    chemical_id BIGINT NOT NULL REFERENCES products(id),
    offer_price DECIMAL(10,2) NOT NULL,
    discount_percentage DECIMAL(5,2),
    offer_valid_until DATE,
    
    -- Template
    template_id BIGINT REFERENCES templates(id),
    subject_override VARCHAR(255), -- Override template subject
    
    -- Recipient Selection
    recipient_selection_type VARCHAR(50) NOT NULL, 
        -- 'MANUAL', 'FILTER', 'CSV_UPLOAD'
    recipient_filter_criteria JSONB, 
        -- e.g., {"chemical_purchased": [1,2,3], "region": "Asia"}
    recipient_count INT DEFAULT 0,
    
    -- Status & Scheduling
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', 
        -- 'DRAFT', 'SCHEDULED', 'SENDING', 'COMPLETED', 'CANCELLED', 'FAILED'
    scheduled_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    
    -- Ownership
    created_by BIGINT NOT NULL REFERENCES users(id),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    
    -- Metadata
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    -- Indexes
    INDEX idx_status (status),
    INDEX idx_scheduled_at (scheduled_at),
    INDEX idx_created_by (created_by),
    INDEX idx_company_id (company_id),
    INDEX idx_created_at (created_at DESC)
);

-- Trigger for updated_at
CREATE TRIGGER update_campaigns_updated_at 
    BEFORE UPDATE ON campaigns 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
```

#### 5.2.2 campaign_recipients
```sql
CREATE TABLE campaign_recipients (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    
    -- Recipient Info (denormalized for performance)
    customer_id BIGINT REFERENCES customers(id),
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    company_name VARCHAR(255),
    
    -- Template Assignment
    template_id BIGINT REFERENCES templates(id),
    template_variables JSONB, 
        -- e.g., {"chemicalName": "Sulfuric Acid", "offerPrice": "500"}
    
    -- Status
    status VARCHAR(50) DEFAULT 'PENDING', 
        -- 'PENDING', 'SENT', 'FAILED', 'BOUNCED', 'UNSUBSCRIBED'
    sent_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_reason TEXT,
    
    -- Tracking
    opened_at TIMESTAMP,
    open_count INT DEFAULT 0,
    clicked_at TIMESTAMP,
    click_count INT DEFAULT 0,
    replied_at TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT NOW(),
    
    -- Indexes
    INDEX idx_campaign_id (campaign_id),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_customer_id (customer_id),
    
    -- Unique constraint to prevent duplicate recipients in same campaign
    UNIQUE (campaign_id, email)
);
```

#### 5.2.3 templates
```sql
CREATE TABLE templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100), -- 'OFFER', 'NEWSLETTER', 'FOLLOW_UP'
    
    -- Email Content
    subject_template VARCHAR(500) NOT NULL, 
        -- e.g., "Exclusive offer on {{chemicalName}} - {{discountPercentage}}% OFF"
    body_html TEXT NOT NULL, -- Full HTML template
    body_text TEXT, -- Plain text fallback
    
    -- Template Variables
    required_variables JSONB, 
        -- e.g., ["firstName", "companyName", "chemicalName", "offerPrice"]
    
    -- Personalization
    tone VARCHAR(50), -- 'FORMAL', 'FRIENDLY', 'CASUAL'
    language VARCHAR(10) DEFAULT 'en', -- 'en', 'hi', etc.
    
    -- Storage
    s3_bucket VARCHAR(255),
    s3_key VARCHAR(500), -- Path to template file in S3
    
    -- Versioning
    version INT DEFAULT 1,
    parent_template_id BIGINT REFERENCES templates(id),
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    is_default BOOLEAN DEFAULT FALSE,
    
    -- Usage Stats
    usage_count INT DEFAULT 0,
    last_used_at TIMESTAMP,
    
    -- Metadata
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    INDEX idx_category (category),
    INDEX idx_is_active (is_active)
);
```

#### 5.2.4 email_jobs
```sql
CREATE TABLE email_jobs (
    id BIGSERIAL PRIMARY KEY,
    
    -- Reference
    campaign_id BIGINT NOT NULL REFERENCES campaigns(id),
    recipient_id BIGINT NOT NULL REFERENCES campaign_recipients(id),
    
    -- Email Details
    from_email VARCHAR(255) NOT NULL,
    to_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body_html TEXT NOT NULL,
    body_text TEXT,
    
    -- SES Details
    ses_message_id VARCHAR(255), -- Returned by SES after send
    
    -- Status
    status VARCHAR(50) DEFAULT 'QUEUED', 
        -- 'QUEUED', 'PROCESSING', 'SENT', 'FAILED', 'BOUNCED', 'COMPLAINED'
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    
    -- Timestamps
    queued_at TIMESTAMP DEFAULT NOW(),
    processing_at TIMESTAMP,
    sent_at TIMESTAMP,
    failed_at TIMESTAMP,
    
    -- Error Handling
    error_message TEXT,
    error_code VARCHAR(100),
    
    -- Tracking IDs
    tracking_pixel_id UUID DEFAULT gen_random_uuid(),
    
    created_at TIMESTAMP DEFAULT NOW(),
    
    -- Indexes
    INDEX idx_campaign_id (campaign_id),
    INDEX idx_status (status),
    INDEX idx_queued_at (queued_at),
    INDEX idx_ses_message_id (ses_message_id),
    INDEX idx_tracking_pixel_id (tracking_pixel_id)
)
PARTITION BY RANGE (created_at);

-- Create monthly partitions
CREATE TABLE email_jobs_2026_07 PARTITION OF email_jobs
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE email_jobs_2026_08 PARTITION OF email_jobs
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
-- Continue for 12 months...
```

#### 5.2.5 email_tracking_events
```sql
CREATE TABLE email_tracking_events (
    id BIGSERIAL PRIMARY KEY,
    
    email_job_id BIGINT NOT NULL REFERENCES email_jobs(id),
    campaign_id BIGINT NOT NULL REFERENCES campaigns(id),
    recipient_id BIGINT NOT NULL REFERENCES campaign_recipients(id),
    
    event_type VARCHAR(50) NOT NULL, 
        -- 'OPEN', 'CLICK', 'BOUNCE', 'COMPLAINT', 'DELIVERY', 'REJECT'
    
    -- Event Details
    link_url TEXT, -- For CLICK events
    user_agent TEXT,
    ip_address INET,
    location JSONB, -- GeoIP data: {"country": "IN", "city": "Mumbai"}
    
    -- SES Event Data (from SNS notifications)
    ses_event_data JSONB,
    
    event_timestamp TIMESTAMP DEFAULT NOW(),
    
    -- Indexes
    INDEX idx_email_job_id (email_job_id),
    INDEX idx_campaign_id (campaign_id),
    INDEX idx_event_type (event_type),
    INDEX idx_event_timestamp (event_timestamp DESC)
)
PARTITION BY RANGE (event_timestamp);
```

#### 5.2.6 email_replies
```sql
CREATE TABLE email_replies (
    id BIGSERIAL PRIMARY KEY,
    
    -- Reference
    campaign_id BIGINT REFERENCES campaigns(id),
    email_job_id BIGINT REFERENCES email_jobs(id),
    customer_id BIGINT REFERENCES customers(id),
    
    -- Email Details
    from_email VARCHAR(255) NOT NULL,
    to_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    body_text TEXT,
    body_html TEXT,
    
    -- Threading
    in_reply_to VARCHAR(255), -- Message-ID from original email
    references TEXT, -- All Message-IDs in thread
    thread_id VARCHAR(255), -- Custom thread identifier
    
    -- Attachments
    has_attachments BOOLEAN DEFAULT FALSE,
    attachments JSONB, -- [{"filename": "quote.pdf", "s3_key": "..."}]
    
    -- Storage
    s3_bucket VARCHAR(255),
    s3_key VARCHAR(500), -- Full raw email in S3
    
    -- Status
    is_read BOOLEAN DEFAULT FALSE,
    is_flagged BOOLEAN DEFAULT FALSE,
    assigned_to BIGINT REFERENCES users(id), -- Sales person
    
    -- Sentiment Analysis (future enhancement)
    sentiment VARCHAR(50), -- 'POSITIVE', 'NEUTRAL', 'NEGATIVE'
    contains_order BOOLEAN, -- AI detection of purchase intent
    
    received_at TIMESTAMP DEFAULT NOW(),
    read_at TIMESTAMP,
    
    -- Indexes
    INDEX idx_campaign_id (campaign_id),
    INDEX idx_from_email (from_email),
    INDEX idx_thread_id (thread_id),
    INDEX idx_received_at (received_at DESC),
    INDEX idx_is_read (is_read)
);
```

#### 5.2.7 outbox_events
```sql
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    
    -- Event Details
    event_type VARCHAR(100) NOT NULL, 
        -- 'CAMPAIGN_CREATED', 'EMAIL_SEND_REQUESTED', 'REPLY_RECEIVED'
    aggregate_id BIGINT NOT NULL, -- campaign_id or email_job_id
    aggregate_type VARCHAR(100) NOT NULL, -- 'CAMPAIGN', 'EMAIL_JOB'
    
    -- Payload
    payload JSONB NOT NULL, 
        -- Full event data to publish to SQS
    
    -- Publishing Status
    published BOOLEAN DEFAULT FALSE,
    published_at TIMESTAMP,
    publish_attempts INT DEFAULT 0,
    
    -- Error Handling
    error_message TEXT,
    
    created_at TIMESTAMP DEFAULT NOW(),
    
    -- Indexes
    INDEX idx_published (published, created_at) WHERE NOT published,
    INDEX idx_aggregate (aggregate_type, aggregate_id)
);
```

#### 5.2.8 unsubscribe_list
```sql
CREATE TABLE unsubscribe_list (
    id BIGSERIAL PRIMARY KEY,
    
    email VARCHAR(255) NOT NULL UNIQUE,
    customer_id BIGINT REFERENCES customers(id),
    
    -- Unsubscribe Details
    unsubscribed_from_campaign_id BIGINT REFERENCES campaigns(id),
    reason VARCHAR(500),
    unsubscribe_ip INET,
    
    unsubscribed_at TIMESTAMP DEFAULT NOW(),
    
    INDEX idx_email (email)
);
```

#### 5.2.9 audit_logs
```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    
    -- User
    user_id BIGINT REFERENCES users(id),
    user_email VARCHAR(255),
    user_ip INET,
    
    -- Action
    action VARCHAR(100) NOT NULL, 
        -- 'CAMPAIGN_CREATED', 'CAMPAIGN_SENT', 'TEMPLATE_EDITED'
    entity_type VARCHAR(100), -- 'CAMPAIGN', 'TEMPLATE'
    entity_id BIGINT,
    
    -- Details
    old_values JSONB,
    new_values JSONB,
    
    timestamp TIMESTAMP DEFAULT NOW(),
    
    INDEX idx_user_id (user_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_timestamp (timestamp DESC)
);
```

### 5.3 Database Optimization Strategies

#### 5.3.1 Partitioning
- Partition `email_jobs` and `email_tracking_events` by month
- Automatically create new partitions via cron job
- Drop partitions older than 12 months (after archiving to S3)

#### 5.3.2 Indexing Strategy
- B-tree indexes on foreign keys and frequently queried columns
- GIN indexes on JSONB columns for JSON queries
- Partial indexes where appropriate (e.g., `WHERE NOT published`)

#### 5.3.3 Connection Pooling
```properties
# HikariCP Configuration
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

#### 5.3.4 Query Optimization
- Use `SELECT DISTINCT ON` for latest status queries
- Leverage CTEs for complex multi-step queries
- Use `EXPLAIN ANALYZE` to identify slow queries
- Implement database query logging for queries >500ms

#### 5.3.5 Caching Strategy
- Redis cache for:
  - Active templates (1 hour TTL)
  - Campaign summary statistics (5 minutes TTL)
  - Unsubscribe list (15 minutes TTL)
- Implement cache-aside pattern
- Use Spring Cache annotations

---

## 6. Detailed Implementation Phases

### Phase 1: Foundation & Setup (Week 1-2)

#### Day 1-3: Environment Setup
- [ ] **Task 1.1**: Set up AWS account and IAM roles
  - Create IAM user for development with appropriate permissions
  - Set up MFA for root account
  - Create IAM roles for ECS tasks, Lambda functions
  - Configure AWS CLI on local machine
  
- [ ] **Task 1.2**: Set up development environment
  - Install Java 17 JDK
  - Install Docker Desktop
  - Install PostgreSQL 15 locally
  - Install IDE (IntelliJ IDEA / VS Code)
  - Install Postman for API testing
  
- [ ] **Task 1.3**: Create Spring Boot project structure
  ```bash
  # Add to existing ChemOS pom.xml
  <modules>
      <module>chemos-core</module>
      <module>chemos-campaigns</module>  <!-- NEW -->
  </modules>
  ```
  
- [ ] **Task 1.4**: Set up version control
  - Create feature branch: `feature/email-campaigns`
  - Set up Git hooks for pre-commit checks
  - Configure .gitignore for sensitive files

#### Day 4-7: Database Setup
- [ ] **Task 2.1**: Create database schemas
  - Run DDL scripts for all tables
  - Set up database migrations with Flyway
  - Create seed data for testing
  
- [ ] **Task 2.2**: Set up RDS instance
  - Provision PostgreSQL RDS instance (db.t3.medium for dev)
  - Configure security groups
  - Enable automated backups
  - Create database user with limited permissions
  
- [ ] **Task 2.3**: Implement database access layer
  - Create JPA entities for all tables
  - Implement Spring Data repositories
  - Write unit tests for repositories
  - Set up H2 in-memory database for testing

#### Day 8-10: Core Domain Models
- [ ] **Task 3.1**: Implement domain entities
  ```java
  @Entity
  @Table(name = "campaigns")
  public class Campaign {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;
      
      private String name;
      private String description;
      
      @ManyToOne
      @JoinColumn(name = "chemical_id")
      private Product chemical;
      
      private BigDecimal offerPrice;
      private BigDecimal discountPercentage;
      
      @Enumerated(EnumType.STRING)
      private CampaignStatus status;
      
      // ... getters, setters, builder
  }
  ```
  
- [ ] **Task 3.2**: Implement DTOs
  - CreateCampaignRequest
  - CampaignResponse
  - CampaignAnalyticsResponse
  - TemplateRequest/Response
  - RecipientRequest/Response
  
- [ ] **Task 3.3**: Implement mappers
  - Use MapStruct for entity-DTO mapping
  - Write unit tests for mappers

#### Day 11-14: AWS Infrastructure Setup
- [ ] **Task 4.1**: Set up S3 buckets
  - Create buckets with versioning enabled
  - Configure CORS for template uploads
  - Set up lifecycle policies
  - Create IAM policies for bucket access
  
- [ ] **Task 4.2**: Set up SQS queues
  - Create all required queues (standard and FIFO)
  - Configure dead-letter queues
  - Set up CloudWatch alarms for queue depth
  
- [ ] **Task 4.3**: Set up SES
  - Verify domain (campaigns.chemos.com)
  - Set up DKIM, SPF, DMARC records
  - Request production access (move out of sandbox)
  - Create email templates in SES
  - Set up bounce and complaint SNS topics

### Phase 2: Core Campaign Functionality (Week 3-4)

#### Day 15-18: Campaign Service Implementation
- [ ] **Task 5.1**: Implement CampaignService
  ```java
  @Service
  @Transactional
  public class CampaignService {
      
      public CampaignResponse createCampaign(CreateCampaignRequest request) {
          // 1. Validate input
          // 2. Create campaign entity
          // 3. Create campaign recipients
          // 4. Create outbox events
          // 5. Return response
      }
      
      public void scheduleCampaign(Long campaignId, LocalDateTime scheduledAt) {
          // Schedule campaign for future sending
      }
      
      public void cancelCampaign(Long campaignId) {
          // Cancel scheduled campaign
      }
  }
  ```
  
- [ ] **Task 5.2**: Implement RecipientService
  ```java
  @Service
  public class RecipientService {
      
      public List<Customer> filterRecipients(RecipientFilterCriteria criteria) {
          // Build dynamic query based on criteria
          // Apply filters: chemical purchased, region, etc.
          // Return filtered customer list
      }
      
      public void validateEmailAddresses(List<String> emails) {
          // Validate email format
          // Check against unsubscribe list
          // Remove duplicates
      }
  }
  ```
  
- [ ] **Task 5.3**: Implement TemplateService
  - Upload template to S3
  - Fetch template from S3
  - Validate template variables
  - Render template with test data

#### Day 19-22: Outbox Pattern Implementation
- [ ] **Task 6.1**: Implement OutboxPublisher
  ```java
  @Service
  public class OutboxPublisher {
      
      @Scheduled(fixedDelay = 5000) // Every 5 seconds
      public void publishPendingEvents() {
          List<OutboxEvent> pending = outboxRepository
              .findByPublishedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100));
          
          for (OutboxEvent event : pending) {
              try {
                  sqsTemplate.send(event.getEventType(), event.getPayload());
                  event.setPublished(true);
                  event.setPublishedAt(Instant.now());
              } catch (Exception e) {
                  event.incrementAttempts();
                  event.setErrorMessage(e.getMessage());
              }
              outboxRepository.save(event);
          }
      }
  }
  ```
  
- [ ] **Task 6.2**: Implement transactional event creation
  - Use `@Transactional` to ensure atomicity
  - Create outbox event in same transaction as campaign
  
- [ ] **Task 6.3**: Write integration tests
  - Test campaign creation with event publishing
  - Test retry logic for failed publishes
  - Test DLQ behavior

#### Day 23-28: Campaign API Endpoints
- [ ] **Task 7.1**: Implement REST controllers
  ```java
  @RestController
  @RequestMapping("/api/v1/campaigns")
  public class CampaignController {
      
      @PostMapping
      public ResponseEntity<CampaignResponse> createCampaign(
          @Valid @RequestBody CreateCampaignRequest request) {
          CampaignResponse response = campaignService.createCampaign(request);
          return ResponseEntity.status(HttpStatus.CREATED).body(response);
      }
      
      @GetMapping("/{id}")
      public ResponseEntity<CampaignResponse> getCampaign(@PathVariable Long id) {
          return ResponseEntity.ok(campaignService.getCampaign(id));
      }
      
      @PutMapping("/{id}/schedule")
      public ResponseEntity<Void> scheduleCampaign(
          @PathVariable Long id,
          @RequestBody ScheduleRequest request) {
          campaignService.scheduleCampaign(id, request.getScheduledAt());
          return ResponseEntity.noContent().build();
      }
      
      @DeleteMapping("/{id}")
      public ResponseEntity<Void> cancelCampaign(@PathVariable Long id) {
          campaignService.cancelCampaign(id);
          return ResponseEntity.noContent().build();
      }
  }
  ```
  
- [ ] **Task 7.2**: Implement input validation
  - Use Bean Validation annotations
  - Custom validators for business rules
  - Global exception handler
  
- [ ] **Task 7.3**: Add security
  - Integrate with existing ChemOS auth
  - Role-based access control (only MD/Sales can create campaigns)
  - Audit logging for all operations

### Phase 3: Email Worker Service (Week 5-6)

#### Day 29-33: Email Worker Implementation
- [ ] **Task 8.1**: Create separate Spring Boot application for workers
  ```
  chemos-email-worker/
  ├── src/
  │   ├── main/
  │   │   ├── java/
  │   │   │   └── com.chemos.emailworker/
  │   │   │       ├── EmailWorkerApplication.java
  │   │   │       ├── config/
  │   │   │       ├── service/
  │   │   │       │   ├── SQSListenerService.java
  │   │   │       │   ├── TemplateRenderService.java
  │   │   │       │   ├── EmailSenderService.java
  │   │   │       │   └── TrackingService.java
  │   │   │       └── model/
  │   │   └── resources/
  │   │       └── application.yml
  │   └── test/
  └── Dockerfile
  ```
  
- [ ] **Task 8.2**: Implement SQS listener
  ```java
  @Service
  public class SQSListenerService {
      
      @SqsListener(value = "${sqs.queue.email-send}")
      public void processEmailJob(String message) {
          EmailJob job = objectMapper.readValue(message, EmailJob.class);
          
          try {
              // 1. Fetch template
              String template = s3Service.getTemplate(job.getTemplateId());
              
              // 2. Render HTML
              String html = templateRenderService.render(template, job.getVariables());
              
              // 3. Inject tracking
              html = trackingService.injectTracking(html, job.getId());
              
              // 4. Send via SES
              String messageId = emailSenderService.send(
                  job.getFromEmail(),
                  job.getToEmail(),
                  job.getSubject(),
                  html
              );
              
              // 5. Update status
              emailJobRepository.updateStatus(job.getId(), "SENT", messageId);
              
          } catch (Exception e) {
              handleFailure(job, e);
          }
      }
      
      private void handleFailure(EmailJob job, Exception e) {
          int retryCount = job.getRetryCount() + 1;
          
          if (retryCount < job.getMaxRetries()) {
              // Re-queue with delay
              sqsTemplate.send(
                  "campaign-email-retry", 
                  job, 
                  calculateBackoff(retryCount)
              );
          } else {
              // Move to DLQ and mark as failed
              emailJobRepository.updateStatus(
                  job.getId(), 
                  "FAILED", 
                  e.getMessage()
              );
          }
      }
  }
  ```
  
- [ ] **Task 8.3**: Implement template rendering
  ```java
  @Service
  public class TemplateRenderService {
      
      private final TemplateEngine templateEngine;
      
      public String render(String templateHtml, Map<String, Object> variables) {
          Context context = new Context();
          context.setVariables(variables);
          
          return templateEngine.process(templateHtml, context);
      }
  }
  ```
  
- [ ] **Task 8.4**: Implement SES sender
  ```java
  @Service
  public class EmailSenderService {
      
      private final AmazonSimpleEmailService sesClient;
      
      public String send(String from, String to, String subject, String html) {
          SendEmailRequest request = new SendEmailRequest()
              .withSource(from)
              .withDestination(new Destination().withToAddresses(to))
              .withMessage(new Message()
                  .withSubject(new Content().withData(subject))
                  .withBody(new Body().withHtml(new Content().withData(html)))
              );
          
          SendEmailResult result = sesClient.sendEmail(request);
          return result.getMessageId();
      }
  }
  ```

#### Day 34-38: Tracking Implementation
- [ ] **Task 9.1**: Implement tracking pixel injection
  ```java
  @Service
  public class TrackingService {
      
      public String injectTracking(String html, Long emailJobId) {
          // 1. Inject tracking pixel
          String pixelUrl = String.format(
              "https://track.chemos.com/open/%s",
              emailJobId
          );
          String pixel = String.format(
              "<img src=\"%s\" width=\"1\" height=\"1\" />",
              pixelUrl
          );
          html = html.replace("</body>", pixel + "</body>");
          
          // 2. Wrap all links for click tracking
          html = wrapLinksForTracking(html, emailJobId);
          
          return html;
      }
      
      private String wrapLinksForTracking(String html, Long emailJobId) {
          // Parse HTML and replace all <a href="..."> with tracking URLs
          // Use Jsoup for HTML parsing
      }
  }
  ```
  
- [ ] **Task 9.2**: Create tracking pixel Lambda
  ```python
  import boto3
  import json
  from datetime import datetime
  
  dynamodb = boto3.resource('dynamodb')
  table = dynamodb.Table('email_tracking_events')
  
  def lambda_handler(event, context):
      email_job_id = event['pathParameters']['emailJobId']
      ip_address = event['requestContext']['identity']['sourceIp']
      user_agent = event['headers'].get('User-Agent', '')
      
      # Record open event
      table.put_item(Item={
          'email_job_id': email_job_id,
          'event_type': 'OPEN',
          'ip_address': ip_address,
          'user_agent': user_agent,
          'timestamp': datetime.utcnow().isoformat()
      })
      
      # Return 1x1 transparent PNG
      return {
          'statusCode': 200,
          'headers': {
              'Content-Type': 'image/png',
              'Cache-Control': 'no-cache, no-store, must-revalidate'
          },
          'body': base64.b64encode(TRANSPARENT_PNG).decode('utf-8'),
          'isBase64Encoded': True
      }
  ```
  
- [ ] **Task 9.3**: Create link click Lambda
  - Similar to tracking pixel but with redirect
  - Record click event and redirect to original URL

#### Day 39-42: Docker & ECS Deployment
- [ ] **Task 10.1**: Create Dockerfile
  ```dockerfile
  FROM openjdk:17-jdk-slim
  
  WORKDIR /app
  
  COPY target/chemos-email-worker.jar app.jar
  
  EXPOSE 8080
  
  ENTRYPOINT ["java", "-jar", "app.jar"]
  ```
  
- [ ] **Task 10.2**: Build and test Docker image locally
  ```bash
  docker build -t chemos-email-worker:latest .
  docker run -p 8080:8080 chemos-email-worker:latest
  ```
  
- [ ] **Task 10.3**: Push to AWS ECR
  ```bash
  aws ecr create-repository --repository-name chemos-email-worker
  docker tag chemos-email-worker:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/chemos-email-worker:latest
  docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/chemos-email-worker:latest
  ```
  
- [ ] **Task 10.4**: Create ECS task definition and service
  - Use Fargate launch type
  - Configure auto-scaling based on SQS queue depth
  - Set up CloudWatch logs

### Phase 4: Reply Handling (Week 7)

#### Day 43-46: SES Inbound Setup
- [ ] **Task 11.1**: Configure SES inbound rule set
  ```
  Rule: Campaign Replies
    Recipients: campaigns@chemos.com, reply@chemos.com
    Actions:
      1. S3 Action → Store in s3://chemos-email-inbound/
      2. Lambda Action → Invoke reply-processor-lambda
  ```
  
- [ ] **Task 11.2**: Create reply processor Lambda
  ```python
  import boto3
  import email
  from email import policy
  import json
  
  s3 = boto3.client('s3')
  dynamodb = boto3.resource('dynamodb')
  
  def lambda_handler(event, context):
      # 1. Get email from S3
      record = event['Records'][0]
      bucket = record['s3']['bucket']['name']
      key = record['s3']['object']['key']
      
      obj = s3.get_object(Bucket=bucket, Key=key)
      raw_email = obj['Body'].read()
      
      # 2. Parse email
      msg = email.message_from_bytes(raw_email, policy=policy.default)
      
      # 3. Extract details
      from_email = msg['From']
      subject = msg['Subject']
      in_reply_to = msg['In-Reply-To']
      
      # Extract campaign ID from custom header or email address
      campaign_id = extract_campaign_id(msg)
      
      # 4. Store in database
      reply_data = {
          'campaign_id': campaign_id,
          'from_email': from_email,
          'subject': subject,
          'body': get_email_body(msg),
          's3_key': key,
          'received_at': datetime.utcnow().isoformat()
      }
      
      # 5. Publish to SQS for async processing
      sqs.send_message(
          QueueUrl=REPLY_QUEUE_URL,
          MessageBody=json.dumps(reply_data)
      )
      
      return {'statusCode': 200}
  ```
  
- [ ] **Task 11.3**: Implement reply notification service
  - Send email to sales team when reply received
  - Push notification to ChemOS UI (WebSocket)

#### Day 47-49: Reply Management API
- [ ] **Task 12.1**: Implement reply endpoints
  ```java
  @RestController
  @RequestMapping("/api/v1/replies")
  public class ReplyController {
      
      @GetMapping("/campaign/{campaignId}")
      public ResponseEntity<Page<ReplyResponse>> getRepliesByCampaign(
          @PathVariable Long campaignId,
          Pageable pageable) {
          return ResponseEntity.ok(replyService.getReplies(campaignId, pageable));
      }
      
      @PutMapping("/{id}/read")
      public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
          replyService.markAsRead(id);
          return ResponseEntity.noContent().build();
      }
  }
  ```

### Phase 5: Analytics & Reporting (Week 8)

#### Day 50-54: Analytics Service
- [ ] **Task 13.1**: Implement analytics aggregation
  ```java
  @Service
  public class AnalyticsService {
      
      public CampaignAnalytics getCampaignAnalytics(Long campaignId) {
          Campaign campaign = campaignRepository.findById(campaignId)
              .orElseThrow();
          
          return CampaignAnalytics.builder()
              .totalRecipients(campaign.getRecipientCount())
              .sentCount(getSentCount(campaignId))
              .deliveredCount(getDeliveredCount(campaignId))
              .openedCount(getOpenedCount(campaignId))
              .clickedCount(getClickedCount(campaignId))
              .repliedCount(getRepliedCount(campaignId))
              .bouncedCount(getBouncedCount(campaignId))
              .openRate(calculateOpenRate(campaignId))
              .clickRate(calculateClickRate(campaignId))
              .conversionRate(calculateConversionRate(campaignId))
              .build();
      }
  }
  ```
  
- [ ] **Task 13.2**: Implement event aggregation job
  - Scheduled job to aggregate tracking events
  - Update campaign_recipients table with latest stats
  - Cache results in Redis

#### Day 55-56: Data Export
- [ ] **Task 14.1**: Implement CSV export
  ```java
  @GetMapping("/campaigns/{id}/export")
  public ResponseEntity<Resource> exportCampaignData(@PathVariable Long id) {
      byte[] csv = analyticsService.exportToCsv(id);
      
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=campaign-" + id + ".csv")
          .contentType(MediaType.parseMediaType("text/csv"))
          .body(new ByteArrayResource(csv));
  }
  ```

### Phase 6: Frontend Development (Week 9-10)

#### Day 57-61: Campaign Creation UI
- [ ] **Task 15.1**: Design campaign creation wizard
  ```
  Step 1: Basic Info (name, description, chemical, offer price)
  Step 2: Select Recipients (filters or CSV upload)
  Step 3: Choose Template (preview available)
  Step 4: Schedule (send now or later)
  Step 5: Review & Confirm (preview email)
  ```
  
- [ ] **Task 15.2**: Implement React components
  ```typescript
  // CampaignWizard.tsx
  const CampaignWizard: React.FC = () => {
      const [step, setStep] = useState(1);
      const [campaignData, setCampaignData] = useState<CampaignFormData>({});
      
      const handleNext = () => setStep(step + 1);
      const handleBack = () => setStep(step - 1);
      
      const handleSubmit = async () => {
          const response = await campaignApi.create(campaignData);
          navigate(`/campaigns/${response.id}`);
      };
      
      return (
          <Stepper activeStep={step - 1}>
              {step === 1 && <BasicInfoStep data={campaignData} onChange={setCampaignData} onNext={handleNext} />}
              {step === 2 && <RecipientStep data={campaignData} onChange={setCampaignData} onNext={handleNext} onBack={handleBack} />}
              {step === 3 && <TemplateStep data={campaignData} onChange={setCampaignData} onNext={handleNext} onBack={handleBack} />}
              {step === 4 && <ScheduleStep data={campaignData} onChange={setCampaignData} onNext={handleNext} onBack={handleBack} />}
              {step === 5 && <ReviewStep data={campaignData} onSubmit={handleSubmit} onBack={handleBack} />}
          </Stepper>
      );
  };
  ```

#### Day 62-66: Analytics Dashboard
- [ ] **Task 16.1**: Create dashboard components
  - Campaign list with status
  - Campaign analytics cards (sent, opened, clicked, replied)
  - Charts (open rate over time, click heatmap)
  - Recipient detail table with individual tracking

#### Day 67-70: Template Management UI
- [ ] **Task 17.1**: Template editor
  - Rich text editor for HTML
  - Variable insertion ({{firstName}}, etc.)
  - Live preview
  - Test email sending

### Phase 7: Testing (Week 11)

#### Day 71-73: Unit Testing
- [ ] **Task 18.1**: Write unit tests for all services
  - CampaignService (100% coverage target)
  - RecipientService
  - TemplateService
  - AnalyticsService
  
- [ ] **Task 18.2**: Write repository tests
  - Test complex queries
  - Test partition pruning

#### Day 74-76: Integration Testing
- [ ] **Task 19.1**: API integration tests
  ```java
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureMockMvc
  class CampaignControllerIntegrationTest {
      
      @Test
      void createCampaign_shouldReturnCreated() {
          CreateCampaignRequest request = CreateCampaignRequest.builder()
              .name("Summer Sale")
              .chemicalId(1L)
              .offerPrice(new BigDecimal("500.00"))
              .build();
          
          mockMvc.perform(post("/api/v1/campaigns")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.id").exists())
              .andExpect(jsonPath("$.name").value("Summer Sale"));
      }
  }
  ```
  
- [ ] **Task 19.2**: End-to-end tests
  - Test full campaign creation → sending → tracking flow
  - Use Testcontainers for PostgreSQL and LocalStack for AWS

#### Day 77: Load Testing
- [ ] **Task 20.1**: JMeter/Gatling load tests
  - Test 100 concurrent campaign creations
  - Test 10,000 emails sent in 10 minutes
  - Test analytics query performance under load

### Phase 8: Security Hardening (Week 12)

#### Day 78-80: Security Implementation
- [ ] **Task 21.1**: Input validation and sanitization
  - Validate all user inputs
  - Sanitize HTML templates to prevent XSS
  - Rate limiting on API endpoints
  
- [ ] **Task 21.2**: Secrets management
  - Move all secrets to AWS Secrets Manager
  - Rotate database passwords
  - Implement least-privilege IAM policies
  
- [ ] **Task 21.3**: Encryption
  - Enable RDS encryption at rest
  - Enable S3 bucket encryption
  - Force HTTPS for all API calls

#### Day 81-82: Penetration Testing
- [ ] **Task 22.1**: OWASP Top 10 checks
  - SQL injection testing
  - XSS testing
  - CSRF protection verification
  - Authentication bypass attempts
  
- [ ] **Task 22.2**: AWS security assessment
  - Run AWS Trusted Advisor
  - Fix security group misconfigurations
  - Enable VPC Flow Logs

#### Day 83-84: Compliance & Documentation
- [ ] **Task 23.1**: CAN-SPAM compliance checklist
  - Include unsubscribe link in all emails
  - Include physical address
  - Honor opt-outs within 10 days
  - Don't use deceptive subject lines
  
- [ ] **Task 23.2**: GDPR considerations (if applicable)
  - Document data processing activities
  - Implement data retention policies
  - Provide data export functionality

---

## 7. Security Implementation

### 7.1 Authentication & Authorization

#### 7.1.1 Leverage Existing ChemOS Auth
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/api/v1/campaigns/**").hasAnyRole("MD", "SALES", "ADMIN")
                .antMatchers("/api/v1/templates/**").hasRole("ADMIN")
                .antMatchers("/api/v1/analytics/**").hasAnyRole("MD", "SALES", "ADMIN")
                .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
                .jwt();
    }
}
```

#### 7.1.2 Role-Based Access Control (RBAC)
- **MD Role**: Full access to all campaigns
- **Sales Role**: Access to own campaigns only
- **Admin Role**: Template management, system configuration

### 7.2 Data Protection

#### 7.2.1 Encryption at Rest
- **RDS**: Enable encryption using AWS KMS
- **S3**: Server-side encryption (SSE-S3 or SSE-KMS)
- **Secrets Manager**: Automatically encrypted

#### 7.2.2 Encryption in Transit
- **API**: Force HTTPS with TLS 1.3
- **Database**: SSL mode required
- **SES**: TLS for email delivery

### 7.3 Input Validation & Sanitization

```java
@NotNull(message = "Campaign name is required")
@Size(min = 3, max = 255, message = "Campaign name must be between 3 and 255 characters")
private String name;

@Email(message = "Invalid email format")
private String recipientEmail;

@DecimalMin(value = "0.01", message = "Offer price must be greater than 0")
private BigDecimal offerPrice;
```

### 7.4 Rate Limiting

```java
@Configuration
public class RateLimitConfig {
    
    @Bean
    public RateLimiter campaignCreationLimiter() {
        return RateLimiter.of("campaignCreation", RateLimiterConfig.custom()
            .limitForPeriod(10) // 10 requests
            .limitRefreshPeriod(Duration.ofMinutes(1)) // per minute
            .timeoutDuration(Duration.ofSeconds(5))
            .build());
    }
}
```

### 7.5 Audit Logging

```java
@Aspect
@Component
public class AuditAspect {
    
    @AfterReturning(pointcut = "execution(* com.chemos.campaigns.service.CampaignService.*(..))", returning = "result")
    public void logAudit(JoinPoint joinPoint, Object result) {
        String action = joinPoint.getSignature().getName();
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        
        auditLogRepository.save(AuditLog.builder()
            .action(action)
            .userId(user)
            .timestamp(Instant.now())
            .details(joinPoint.getArgs())
            .build());
    }
}
```

### 7.6 SQL Injection Prevention
- Use JPA/Hibernate with parameterized queries
- Never concatenate SQL strings
- Use Spring Data repository methods

### 7.7 XSS Prevention
- Sanitize HTML templates using OWASP Java HTML Sanitizer
- Use Content Security Policy headers
- Escape user inputs in frontend

### 7.8 CSRF Protection
- Enable CSRF tokens for state-changing operations
- Use SameSite cookie attribute

---

## 8. Testing Strategy

### 8.1 Unit Testing (Target: 80% Coverage)

```java
@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {
    
    @Mock
    private CampaignRepository campaignRepository;
    
    @Mock
    private OutboxEventRepository outboxRepository;
    
    @InjectMocks
    private CampaignService campaignService;
    
    @Test
    void createCampaign_shouldCreateCampaignAndPublishEvent() {
        // Given
        CreateCampaignRequest request = CreateCampaignRequest.builder()
            .name("Test Campaign")
            .chemicalId(1L)
            .offerPrice(new BigDecimal("100.00"))
            .build();
        
        Campaign savedCampaign = Campaign.builder()
            .id(1L)
            .name("Test Campaign")
            .build();
        
        when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);
        
        // When
        CampaignResponse response = campaignService.createCampaign(request);
        
        // Then
        assertNotNull(response);
        assertEquals("Test Campaign", response.getName());
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }
}
```

### 8.2 Integration Testing

```java
@SpringBootTest
@Testcontainers
class CampaignIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb");
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:latest"))
        .withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.S3);
    
    @Autowired
    private CampaignService campaignService;
    
    @Test
    void endToEndCampaignFlow() {
        // Test full flow: create → schedule → send → track
    }
}
```

### 8.3 Load Testing

```groovy
// Gatling scenario
val createCampaignScenario = scenario("Create Campaign")
    .exec(http("Create Campaign")
        .post("/api/v1/campaigns")
        .header("Authorization", "Bearer ${token}")
        .body(StringBody("""{"name": "Load Test", "chemicalId": 1, "offerPrice": 100}"""))
        .check(status.is(201)))

setUp(
    createCampaignScenario.inject(
        rampUsers(100) during (1 minutes)
    )
).protocols(httpProtocol)
```

### 8.4 Manual Testing Checklist
- [ ] Campaign creation with all fields
- [ ] Campaign creation with minimal fields
- [ ] Recipient filtering
- [ ] CSV upload
- [ ] Template preview
- [ ] Email sending (send test email to yourself)
- [ ] Open tracking (open email in different clients)
- [ ] Click tracking (click links)
- [ ] Reply handling (reply to campaign email)
- [ ] Unsubscribe flow
- [ ] Campaign cancellation
- [ ] Analytics accuracy

---

## 9. Deployment Strategy

### 9.1 Infrastructure as Code (Terraform)

```hcl
# terraform/main.tf
resource "aws_rds_instance" "campaigns" {
  identifier           = "chemos-campaigns-db"
  engine               = "postgres"
  engine_version       = "15.3"
  instance_class       = "db.t3.medium"
  allocated_storage    = 100
  storage_encrypted    = true
  
  db_name  = "campaigns"
  username = var.db_username
  password = var.db_password
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  multi_az = true
  
  tags = {
    Environment = var.environment
    Project     = "ChemOS-Campaigns"
  }
}

resource "aws_sqs_queue" "email_send" {
  name                      = "campaign-email-send.fifo"
  fifo_queue                = true
  content_based_deduplication = true
  
  visibility_timeout_seconds = 300
  message_retention_seconds  = 1209600 # 14 days
  
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.email_dlq.arn
    maxReceiveCount     = 3
  })
}

resource "aws_ecs_cluster" "email_workers" {
  name = "chemos-email-workers"
  
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_ecs_task_definition" "email_worker" {
  family                   = "email-worker"
  requires_compatibilities = ["FARGATE"]
  network_mode            = "awsvpc"
  cpu                     = "256"
  memory                  = "512"
  
  container_definitions = jsonencode([{
    name      = "email-worker"
    image     = "${var.ecr_repository_url}:${var.image_tag}"
    essential = true
    
    environment = [
      {name = "SPRING_PROFILES_ACTIVE", value = var.environment},
      {name = "SQS_QUEUE_URL", value = aws_sqs_queue.email_send.url}
    ]
    
    secrets = [
      {name = "DB_PASSWORD", valueFrom = aws_secretsmanager_secret.db_password.arn}
    ]
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/email-worker"
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])
  
  execution_role_arn = aws_iam_role.ecs_execution_role.arn
  task_role_arn      = aws_iam_role.ecs_task_role.arn
}

resource "aws_appautoscaling_target" "ecs_target" {
  max_capacity       = 20
  min_capacity       = 1
  resource_id        = "service/${aws_ecs_cluster.email_workers.name}/${aws_ecs_service.email_worker.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "scale_on_queue_depth" {
  name               = "scale-on-queue-depth"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace
  
  target_tracking_scaling_policy_configuration {
    target_value = 100.0
    
    customized_metric_specification {
      metric_name = "ApproximateNumberOfMessagesVisible"
      namespace   = "AWS/SQS"
      statistic   = "Average"
      
      dimensions {
        name  = "QueueName"
        value = aws_sqs_queue.email_send.name
      }
    }
  }
}
```

### 9.2 CI/CD Pipeline (GitHub Actions)

```yaml
# .github/workflows/deploy.yml
name: Deploy ChemOS Campaigns

on:
  push:
    branches: [main, develop]

env:
  AWS_REGION: us-east-1
  ECR_REPOSITORY: chemos-email-worker

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run tests
        run: mvn clean test
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3

  build-and-deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v1
      
      - name: Build, tag, and push image to ECR
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
      
      - name: Deploy to ECS
        run: |
          aws ecs update-service \
            --cluster chemos-email-workers \
            --service email-worker-service \
            --force-new-deployment
      
      - name: Run database migrations
        run: |
          mvn flyway:migrate \
            -Dflyway.url=${{ secrets.DB_URL }} \
            -Dflyway.user=${{ secrets.DB_USER }} \
            -Dflyway.password=${{ secrets.DB_PASSWORD }}
```

### 9.3 Blue-Green Deployment
- Deploy new version to "green" environment
- Run smoke tests
- Switch traffic from "blue" to "green"
- Keep "blue" for 24 hours for rollback

### 9.4 Rollback Strategy
```bash
# Rollback ECS service to previous task definition
aws ecs update-service \
  --cluster chemos-email-workers \
  --service email-worker-service \
  --task-definition email-worker:previous-revision

# Rollback database migration
mvn flyway:undo -Dflyway.target=<previous-version>
```

---

## 10. Monitoring & Observability

### 10.1 CloudWatch Dashboards

```json
{
  "widgets": [
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/SQS", "ApproximateNumberOfMessagesVisible", {"stat": "Average"}],
          [".", "ApproximateAgeOfOldestMessage", {"stat": "Maximum"}]
        ],
        "period": 60,
        "stat": "Average",
        "region": "us-east-1",
        "title": "SQS Queue Metrics"
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/SES", "Send", {"stat": "Sum"}],
          [".", "Bounce", {"stat": "Sum"}],
          [".", "Complaint", {"stat": "Sum"}]
        ],
        "period": 300,
        "stat": "Sum",
        "region": "us-east-1",
        "title": "SES Sending Metrics"
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/RDS", "CPUUtilization", {"stat": "Average"}],
          [".", "DatabaseConnections", {"stat": "Average"}],
          [".", "ReadLatency", {"stat": "Average"}]
        ],
        "period": 300,
        "stat": "Average",
        "region": "us-east-1",
        "title": "RDS Performance"
      }
    }
  ]
}
```

### 10.2 CloudWatch Alarms

```hcl
resource "aws_cloudwatch_metric_alarm" "high_queue_depth" {
  alarm_name          = "campaign-queue-high-depth"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = "300"
  statistic           = "Average"
  threshold           = "1000"
  alarm_description   = "This metric monitors SQS queue depth"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  
  dimensions = {
    QueueName = aws_sqs_queue.email_send.name
  }
}

resource "aws_cloudwatch_metric_alarm" "high_bounce_rate" {
  alarm_name          = "ses-high-bounce-rate"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "Reputation.BounceRate"
  namespace           = "AWS/SES"
  period              = "3600"
  statistic           = "Average"
  threshold           = "0.05" # 5%
  alarm_description   = "SES bounce rate is too high"
  alarm_actions       = [aws_sns_topic.alerts.arn]
}
```

### 10.3 Application Logging

```java
@Slf4j
@Service
public class EmailSenderService {
    
    public String send(String from, String to, String subject, String html) {
        log.info("Sending email: from={}, to={}, subject={}", from, to, subject);
        
        try {
            SendEmailResult result = sesClient.sendEmail(request);
            log.info("Email sent successfully: messageId={}, to={}", result.getMessageId(), to);
            return result.getMessageId();
        } catch (Exception e) {
            log.error("Failed to send email: to={}, error={}", to, e.getMessage(), e);
            throw new EmailSendException("Failed to send email", e);
        }
    }
}
```

### 10.4 Distributed Tracing (AWS X-Ray)

```java
@Configuration
public class XRayConfig {
    
    @Bean
    public Filter tracingFilter() {
        return new AWSXRayServletFilter("ChemOS-Campaigns");
    }
}
```

### 10.5 Health Checks

```java
@RestController
@RequestMapping("/actuator/health")
public class HealthController {
    
    @GetMapping
    public ResponseEntity<HealthStatus> health() {
        boolean dbHealthy = checkDatabaseHealth();
        boolean sqsHealthy = checkSQSHealth();
        boolean sesHealthy = checkSESHealth();
        
        if (dbHealthy && sqsHealthy && sesHealthy) {
            return ResponseEntity.ok(new HealthStatus("UP"));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new HealthStatus("DOWN"));
        }
    }
}
```

---

## 11. Performance & Scalability

### 11.1 Database Optimization

#### Indexing Strategy
```sql
-- Composite indexes for common queries
CREATE INDEX idx_campaign_recipients_campaign_status 
    ON campaign_recipients(campaign_id, status);

CREATE INDEX idx_email_jobs_campaign_status_created 
    ON email_jobs(campaign_id, status, created_at DESC);

-- Partial index for active campaigns
CREATE INDEX idx_campaigns_active 
    ON campaigns(created_at DESC) 
    WHERE status IN ('DRAFT', 'SCHEDULED', 'SENDING');

-- GIN index for JSONB queries
CREATE INDEX idx_campaign_filter_criteria 
    ON campaigns USING GIN(recipient_filter_criteria);
```

#### Connection Pooling
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000
```

#### Query Optimization
```java
// Bad: N+1 query problem
List<Campaign> campaigns = campaignRepository.findAll();
for (Campaign c : campaigns) {
    int recipientCount = c.getRecipients().size(); // N queries
}

// Good: Fetch join
@Query("SELECT c FROM Campaign c LEFT JOIN FETCH c.recipients WHERE c.id = :id")
Campaign findByIdWithRecipients(@Param("id") Long id);
```

### 11.2 Caching Strategy

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(15))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("templates", config.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("analytics", config.entryTtl(Duration.ofMinutes(5)))
            .build();
    }
}

@Service
public class TemplateService {
    
    @Cacheable(value = "templates", key = "#id")
    public Template getTemplate(Long id) {
        return templateRepository.findById(id).orElseThrow();
    }
    
    @CacheEvict(value = "templates", key = "#template.id")
    public Template updateTemplate(Template template) {
        return templateRepository.save(template);
    }
}
```

### 11.3 Async Processing

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

@Service
public class CampaignService {
    
    @Async
    public CompletableFuture<Void> processRecipientsAsync(Long campaignId) {
        // Process recipients in background
        return CompletableFuture.completedFuture(null);
    }
}
```

### 11.4 Rate Limiting for SES

```java
@Component
public class SESRateLimiter {
    
    private final RateLimiter rateLimiter = RateLimiter.create(14.0); // 14 emails/sec (SES default)
    
    public void sendWithRateLimit(SendEmailRequest request) {
        rateLimiter.acquire(); // Block until permit available
        sesClient.sendEmail(request);
    }
}
```

### 11.5 Batch Processing

```java
@Service
public class RecipientBatchProcessor {
    
    private static final int BATCH_SIZE = 1000;
    
    public void processCampaignRecipients(Long campaignId) {
        int page = 0;
        Page<CampaignRecipient> recipients;
        
        do {
            recipients = recipientRepository.findByCampaignId(
                campaignId, 
                PageRequest.of(page, BATCH_SIZE)
            );
            
            // Process batch
            publishToSQS(recipients.getContent());
            
            page++;
        } while (recipients.hasNext());
    }
}
```

---

## 12. Disaster Recovery & Business Continuity

### 12.1 Backup Strategy

#### Database Backups
- **Automated RDS Snapshots**: Daily at 3 AM UTC, 7-day retention
- **Manual Snapshots**: Before major deployments
- **Cross-region Replication**: Copy snapshots to secondary region

#### S3 Backups
- **Versioning**: Enabled on all buckets
- **Cross-region Replication**: Replicate templates to DR region
- **Lifecycle Policies**: Move old emails to Glacier after 90 days

### 12.2 Disaster Recovery Plan

#### RTO/RPO Targets
- **Recovery Time Objective (RTO)**: 4 hours
- **Recovery Point Objective (RPO)**: 1 hour (based on snapshot frequency)

#### DR Procedure
1. **Database Recovery**
   ```bash
   # Restore from latest snapshot
   aws rds restore-db-instance-from-db-snapshot \
     --db-instance-identifier chemos-campaigns-db-restored \
     --db-snapshot-identifier latest-snapshot
   ```

2. **Application Recovery**
   - Deploy ECS tasks in DR region
   - Update Route53 to point to DR region
   - Verify health checks pass

3. **Data Validation**
   - Run data integrity checks
   - Verify latest campaigns are present
   - Check SQS queue states

### 12.3 High Availability

#### Multi-AZ Deployment
- **RDS**: Multi-AZ for automatic failover
- **ECS**: Tasks spread across multiple AZs
- **SQS**: Inherently multi-AZ

#### Circuit Breaker Pattern
```java
@Service
public class EmailSenderService {
    
    private final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("sesCircuitBreaker");
    
    public String sendWithCircuitBreaker(SendEmailRequest request) {
        return Try.ofSupplier(
            CircuitBreaker.decorateSupplier(circuitBreaker, () -> sesClient.sendEmail(request))
        ).map(SendEmailResult::getMessageId)
         .recover(CallNotPermittedException.class, "Circuit breaker is OPEN")
         .get();
    }
}
```

---

## 13. Cost Analysis

### 13.1 Monthly Cost Breakdown (10K customers, 10 campaigns/month)

| Service | Configuration | Usage | Cost |
|---------|--------------|-------|------|
| **RDS PostgreSQL** | db.t3.medium (Multi-AZ) | 730 hours | $110 |
| **RDS Storage** | 100 GB | 100 GB | $23 |
| **ECS Fargate** | 5 tasks x 0.25 vCPU x 0.5 GB | 3,650 task-hours | $32 |
| **SQS** | Standard queues | 1M requests | $0.40 |
| **SES** | Email sending | 100K emails | $10 |
| **S3** | Templates + inbound emails | 10 GB | $0.23 |
| **CloudWatch** | Logs + metrics | 5 GB logs | $12 |
| **Lambda** | Tracking + reply processing | 100K invocations | $2 |
| **Data Transfer** | Outbound data | 5 GB | $0.45 |
| **Secrets Manager** | 5 secrets | 5 secrets | $2 |
| **Route53** | Hosted zone | 1 zone | $0.50 |
| **TOTAL** | | | **~$192/month** |

### 13.2 Cost per Email
- **Current scale (100K emails/month)**: $0.0019 per email
- **At scale (1M emails/month)**: $0.0015 per email (economies of scale)

### 13.3 Cost Optimization Tips
- Use Spot instances for non-critical workers (70% savings)
- Archive old emails to Glacier (90% storage savings)
- Use reserved capacity for RDS (40% savings)
- Optimize database queries to reduce RDS CPU

---

## 14. Risk Assessment & Mitigation

### 14.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| **SES account suspension** | Medium | High | Implement double opt-in, monitor bounce/complaint rates, warm up sending |
| **Database performance degradation** | Medium | High | Implement caching, read replicas, query optimization, partitioning |
| **Email deliverability issues** | High | Medium | DKIM/SPF/DMARC, dedicated IP, sender reputation monitoring |
| **SQS message loss** | Low | High | Use FIFO queues, DLQ, idempotency checks |
| **Security breach** | Low | Critical | Encryption, IAM policies, penetration testing, audit logs |
| **Cost overrun** | Medium | Medium | Set billing alarms, implement cost tracking, regular reviews |

### 14.2 Business Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| **Spam complaints** | Medium | High | Clear unsubscribe, respect opt-outs, quality content |
| **Regulatory non-compliance** | Low | Critical | Legal review, CAN-SPAM/GDPR compliance checklist |
| **System downtime during campaign** | Low | High | Multi-AZ deployment, automated failover, monitoring |
| **Data loss** | Very Low | Critical | Regular backups, cross-region replication, testing restores |

### 14.3 Mitigation Checklist
- [ ] Set up CloudWatch billing alarms
- [ ] Implement SES bounce/complaint handling
- [ ] Test disaster recovery procedure quarterly
- [ ] Conduct security audit before production launch
- [ ] Get legal approval for email templates
- [ ] Document runbooks for common incidents
- [ ] Set up on-call rotation for production support

---

## 15. Compliance & Legal Considerations

### 15.1 CAN-SPAM Act Compliance (US)

**Requirements**:
- ✅ Don't use false or misleading header information
- ✅ Don't use deceptive subject lines
- ✅ Identify the message as an ad (for marketing emails)
- ✅ Tell recipients where you're located (physical address)
- ✅ Tell recipients how to opt out of future emails
- ✅ Honor opt-out requests promptly (within 10 business days)
- ✅ Monitor what others are doing on your behalf

**Implementation**:
```html
<!-- Email footer template -->
<footer style="margin-top: 40px; padding-top: 20px; border-top: 1px solid #ccc; font-size: 12px; color: #666;">
    <p>
        You are receiving this email because you are a customer of ChemOS.
        If you no longer wish to receive these emails, please 
        <a href="https://campaigns.chemos.com/unsubscribe?token={{unsubscribeToken}}">unsubscribe</a>.
    </p>
    <p>
        {{companyName}}<br>
        {{physicalAddress}}<br>
        Mumbai, Maharashtra 400001, India
    </p>
</footer>
```

### 15.2 GDPR Compliance (EU)

**Requirements** (if sending to EU customers):
- ✅ Lawful basis for processing (consent or legitimate interest)
- ✅ Right to access data
- ✅ Right to erasure ("right to be forgotten")
- ✅ Right to data portability
- ✅ Privacy by design and by default
- ✅ Data breach notification (within 72 hours)

**Implementation**:
```java
@RestController
@RequestMapping("/api/v1/gdpr")
public class GDPRController {
    
    @GetMapping("/my-data")
    public ResponseEntity<CustomerData> getMyData(Authentication auth) {
        // Export all data related to customer
        CustomerData data = gdprService.exportCustomerData(auth.getName());
        return ResponseEntity.ok(data);
    }
    
    @DeleteMapping("/my-data")
    public ResponseEntity<Void> deleteMyData(Authentication auth) {
        // Delete all customer data (right to be forgotten)
        gdprService.deleteCustomerData(auth.getName());
        return ResponseEntity.noContent().build();
    }
}
```

### 15.3 Data Retention Policy

| Data Type | Retention Period | Reason |
|-----------|-----------------|--------|
| Campaign metadata | 7 years | Business records |
| Email content | 3 years | Legal compliance |
| Tracking events | 2 years | Analytics |
| Reply emails | 5 years | Business communication |
| Audit logs | 7 years | Compliance |
| Unsubscribe list | Indefinite | Legal requirement |

### 15.4 Legal Review Checklist
- [ ] Review email template language with legal team
- [ ] Verify physical address is accurate
- [ ] Test unsubscribe link functionality
- [ ] Document consent mechanism for email list
- [ ] Create privacy policy for email campaigns
- [ ] Train sales team on compliance requirements

---

## 16. Day-by-Day Implementation Schedule

### Week 1: Foundation
- **Day 1**: AWS account setup, IAM roles, local environment
- **Day 2**: Spring Boot project structure, version control
- **Day 3**: Database schema design, Flyway migrations
- **Day 4**: RDS instance setup, database access layer
- **Day 5**: JPA entities, repositories, unit tests
- **Day 6**: S3 buckets, SQS queues setup
- **Day 7**: SES domain verification, DKIM/SPF configuration

### Week 2: Core Services
- **Day 8**: Campaign entity and DTOs
- **Day 9**: CampaignService implementation
- **Day 10**: RecipientService implementation
- **Day 11**: TemplateService implementation
- **Day 12**: Outbox pattern implementation
- **Day 13**: Campaign API endpoints
- **Day 14**: API testing with Postman

### Week 3: Email Worker
- **Day 15**: Email worker Spring Boot app skeleton
- **Day 16**: SQS listener implementation
- **Day 17**: Template rendering service (Thymeleaf)
- **Day 18**: SES sender service
- **Day 19**: Error handling and retry logic
- **Day 20**: Tracking pixel and link wrapping
- **Day 21**: Worker unit tests

### Week 4: Worker Deployment
- **Day 22**: Dockerfile creation
- **Day 23**: Local Docker testing
- **Day 24**: ECR repository, push image
- **Day 25**: ECS task definition and service
- **Day 26**: Auto-scaling configuration
- **Day 27**: End-to-end testing (campaign → email sent)
- **Day 28**: Performance tuning

### Week 5: Reply Handling
- **Day 29**: SES inbound rule set configuration
- **Day 30**: Reply processor Lambda function
- **Day 31**: Reply storage in database
- **Day 32**: Reply notification service
- **Day 33**: Reply API endpoints
- **Day 34**: Reply UI components
- **Day 35**: Testing reply flow

### Week 6: Analytics
- **Day 36**: Tracking pixel Lambda (open tracking)
- **Day 37**: Click tracking Lambda
- **Day 38**: Event aggregation service
- **Day 39**: Analytics API endpoints
- **Day 40**: Dashboard components
- **Day 41**: CSV export functionality
- **Day 42**: Analytics testing

### Week 7: Frontend
- **Day 43**: Campaign creation wizard (Step 1-2)
- **Day 44**: Campaign creation wizard (Step 3-5)
- **Day 45**: Campaign list and detail pages
- **Day 46**: Template management UI
- **Day 47**: Analytics dashboard
- **Day 48**: Reply inbox UI
- **Day 49**: UI polish and responsive design

### Week 8: Testing
- **Day 50**: Unit test coverage review (target 80%)
- **Day 51**: Integration tests (API)
- **Day 52**: End-to-end tests (Testcontainers)
- **Day 53**: Load testing (JMeter/Gatling)
- **Day 54**: Bug fixing
- **Day 55**: Manual testing checklist
- **Day 56**: User acceptance testing prep

### Week 9: Security & Compliance
- **Day 57**: Input validation and sanitization
- **Day 58**: Secrets Manager migration
- **Day 59**: Encryption verification
- **Day 60**: OWASP Top 10 security testing
- **Day 61**: CAN-SPAM compliance review
- **Day 62**: GDPR features (if applicable)
- **Day 63**: Security audit and fixes

### Week 10: Infrastructure & Deployment
- **Day 64**: Terraform scripts for all resources
- **Day 65**: GitHub Actions CI/CD pipeline
- **Day 66**: Staging environment setup
- **Day 67**: Deployment testing
- **Day 68**: Monitoring dashboard setup
- **Day 69**: CloudWatch alarms configuration
- **Day 70**: Runbook documentation

### Week 11: Production Prep
- **Day 71**: Production environment provisioning
- **Day 72**: Production deployment
- **Day 73**: Smoke testing in production
- **Day 74**: Load testing in production
- **Day 75**: Disaster recovery testing
- **Day 76**: Training materials for sales team
- **Day 77**: Go-live checklist review

### Week 12: Launch & Monitor
- **Day 78**: Soft launch with 100 test emails
- **Day 79**: Monitor metrics, fix issues
- **Day 80**: Launch with first real campaign (1K recipients)
- **Day 81**: Monitor deliverability and performance
- **Day 82**: Full launch (10K recipients)
- **Day 83**: 24-hour monitoring
- **Day 84**: Post-launch review and optimization

---

## 17. Success Criteria & KPIs

### 17.1 Technical KPIs
- **System Uptime**: 99.9%
- **API Response Time**: <1 second (p95)
- **Email Send Rate**: >500 emails/minute
- **Email Delivery Rate**: >95%
- **Database Query Performance**: <500ms (p95)
- **Test Coverage**: >80%

### 17.2 Business KPIs
- **Campaign Creation Time**: <2 minutes (from start to send)
- **Email Open Rate**: >20% (industry average for B2B)
- **Email Click Rate**: >2.5%
- **Reply Rate**: >1%
- **Unsubscribe Rate**: <0.5%
- **User Satisfaction**: >8/10

### 17.3 Quality Gates
- ✅ All unit tests pass
- ✅ Integration tests pass
- ✅ Load tests meet performance targets
- ✅ Security scan shows no critical vulnerabilities
- ✅ Code review approved
- ✅ Legal compliance verified
- ✅ User acceptance testing completed

---

## 18. Post-Launch Roadmap

### Phase 1 (Month 2-3)
- [ ] A/B testing for templates
- [ ] Advanced recipient segmentation
- [ ] Email template builder (drag-and-drop)
- [ ] Mobile app notifications for replies

### Phase 2 (Month 4-6)
- [ ] AI-powered send time optimization
- [ ] Predictive analytics (which customers likely to respond)
- [ ] Multi-language support
- [ ] Integration with CRM systems

### Phase 3 (Month 7-12)
- [ ] SMS campaign support
- [ ] WhatsApp Business integration
- [ ] Advanced workflow automation
- [ ] Machine learning for sentiment analysis

---

## 19. Support & Maintenance

### 19.1 Monitoring Checklist (Daily)
- [ ] Check CloudWatch dashboard for anomalies
- [ ] Review SQS queue depth
- [ ] Check SES bounce/complaint rates
- [ ] Review application error logs
- [ ] Verify database performance metrics

### 19.2 Maintenance Tasks (Weekly)
- [ ] Review and clear dead letter queue
- [ ] Analyze slow queries
- [ ] Review cost and optimize if needed
- [ ] Check for security updates
- [ ] Backup verification test

### 19.3 Maintenance Tasks (Monthly)
- [ ] Database partition management (create new, archive old)
- [ ] Review and update templates
- [ ] Security patch updates
- [ ] Disaster recovery drill
- [ ] Performance optimization review

### 19.4 On-Call Runbook

**Incident: High bounce rate (>5%)**
1. Stop all campaigns immediately
2. Investigate bounce reasons (hard vs soft)
3. Clean email list
4. Check sender reputation
5. Gradually resume sending

**Incident: Database connection pool exhausted**
1. Check for slow queries
2. Kill long-running queries if needed
3. Increase connection pool size temporarily
4. Investigate root cause
5. Optimize queries

**Incident: SQS queue depth increasing**
1. Check worker health
2. Scale up ECS tasks manually if needed
3. Verify SES sending limits not reached
4. Check for application errors in workers

---

## 20. Conclusion

This comprehensive implementation plan provides a detailed roadmap for building a robust, scalable, and secure email campaign system for ChemOS. Key takeaways:

### 20.1 Critical Success Factors
1. **Reliability First**: Outbox pattern, retries, monitoring
2. **Security by Design**: Encryption, IAM, audit logs
3. **Scalability**: Event-driven architecture, auto-scaling
4. **Cost Efficiency**: Pay-per-use services, optimization
5. **Compliance**: CAN-SPAM, GDPR, legal review

### 20.2 Recommended Next Steps
1. Review this plan with stakeholders
2. Get approval for AWS budget (~$200/month)
3. Set up development environment (Week 1, Day 1)
4. Begin implementation following the day-by-day schedule
5. Schedule weekly check-ins with MD/Sales team for feedback

### 20.3 Resources Needed
- **Developer Time**: 12 weeks full-time
- **AWS Budget**: $200-300/month
- **Legal Review**: 1-2 hours
- **User Testing**: 2-3 sales team members for 4 hours

### 20.4 Expected Outcomes
- Production-ready email campaign system in 12 weeks
- Capable of handling 10K customers initially
- Scalable to 1M+ customers
- Cost-effective at <$0.002 per email
- Enterprise-grade security and reliability

**Good luck with the implementation! This is a well-defined project with clear requirements and a solid architecture. Follow the plan systematically, and you'll have a production-grade system ready in 12 weeks.**

---

## Appendix A: Technology Alternatives Comparison

| Feature | Option A | Option B | Recommendation |
|---------|----------|----------|----------------|
| Email Provider | AWS SES ($0.10/1K) | SendGrid ($0.60/1K) | SES for cost |
| Queue | SQS (managed) | RabbitMQ (self-hosted) | SQS for simplicity |
| Database | PostgreSQL (relational) | DynamoDB (NoSQL) | PostgreSQL for complex queries |
| Workers | ECS Fargate (containers) | Lambda (serverless) | Fargate for long-running tasks |
| Template Engine | Thymeleaf (Java) | Handlebars (JS) | Thymeleaf for Spring integration |

## Appendix B: Sample Email Templates

```html
<!-- Template 1: Friendly Morning -->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="text-align: center; margin-bottom: 30px;">
            <img src="https://chemos.com/logo.png" alt="ChemOS" style="max-width: 200px;">
        </div>
        
        <p>Good morning <strong>{{firstName}}</strong>,</p>
        
        <p>Hope your week is going well at <strong>{{companyName}}</strong>!</p>
        
        <p>We have an exclusive offer on <strong>{{chemicalName}}</strong> that I thought would be perfect for you:</p>
        
        <div style="background-color: #f4f4f4; padding: 20px; border-radius: 5px; margin: 20px 0;">
            <h2 style="color: #0066cc; margin-top: 0;">Special Offer - {{discountPercentage}}% OFF</h2>
            <p style="font-size: 24px; font-weight: bold; color: #00aa00;">
                ₹{{offerPrice}} per {{unit}}
            </p>
            <p>Original Price: <strike>₹{{originalPrice}}</strike></p>
            <p>Valid until: {{validUntil}}</p>
        </div>
        
        <p>
            <a href="{{offerUrl}}" style="display: inline-block; background-color: #0066cc; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">
                View Full Details
            </a>
        </p>
        
        <p>Feel free to reply to this email if you have any questions. I'm here to help!</p>
        
        <p>Best regards,<br>
        <strong>{{salespersonName}}</strong><br>
        {{salespersonTitle}}<br>
        ChemOS</p>
        
        <hr style="border: none; border-top: 1px solid #ccc; margin: 30px 0;">
        
        <p style="font-size: 12px; color: #666;">
            You are receiving this email because you are a valued customer of ChemOS.
            <a href="{{unsubscribeUrl}}">Unsubscribe</a>
        </p>
        
        <p style="font-size: 12px; color: #666;">
            ChemOS Pvt. Ltd.<br>
            123 Business District, Mumbai, Maharashtra 400001, India
        </p>
    </div>
</body>
</html>
```

## Appendix C: Database Migration Script Example

```sql
-- V001__initial_schema.sql
CREATE TABLE campaigns (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    chemical_id BIGINT NOT NULL REFERENCES products(id),
    offer_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_campaigns_status ON campaigns(status);
CREATE INDEX idx_campaigns_created_by ON campaigns(created_by);

-- V002__add_campaign_recipients.sql
CREATE TABLE campaign_recipients (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    customer_id BIGINT REFERENCES customers(id),
    email VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (campaign_id, email)
);

CREATE INDEX idx_campaign_recipients_campaign_id ON campaign_recipients(campaign_id);
```

---

**End of Document**
