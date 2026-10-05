# AWS setup for the STAGE stage (Version 2)

STAGE is a Docker container on an EC2 server with its own Amazon RDS PostgreSQL database
(Submission 3, Section 2). Builds are never rebuilt for STAGE: the **Deploy to STAGE** workflow takes a build that
already passed TEST from your S3 bucket and runs it on the server. Do these steps once, in this order, in **us-east-1**.

Rough cost: a t3.micro server, a db.t4g.micro database and a public IPv4 address. New accounts' free-plan credits
cover this; stop the EC2 instance and the database when you're not using them.

---

## 1. Store the STAGE settings (Systems Manager → Parameter Store)
Search **Systems Manager** → **Parameter Store** → **Create parameter**, five times:

| Name | Type | Value |
|---|---|---|
| `/nsi/stage/DB_NAME` | String | `nsi_inventory` |
| `/nsi/stage/DB_USERNAME` | String | `nsi_app` |
| `/nsi/stage/DB_PASSWORD` | SecureString | a strong password (write it down for step 4) |
| `/nsi/stage/NSI_BOOTSTRAP_ADMIN_PASSWORD` | SecureString | the password the first `admin` login will use on STAGE |
| `/nsi/stage/DB_HOST` | String | `pending` for now – you fill it in at step 5 |

## 2. Role for the STAGE server (IAM)
**IAM → Roles → Create role** → Trusted entity **AWS service**, use case **EC2** → Next.
1. Attach the managed policy **AmazonSSMManagedInstanceCore** → Next → name `nsi-stage-ec2` → **Create role**.
2. Open the role → **Add permissions → Create inline policy → JSON**, paste (put your bucket name in), name it `nsi-stage-read`:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Effect": "Allow", "Action": "s3:GetObject",
      "Resource": ["arn:aws:s3:::YOUR-BUCKET/builds/*", "arn:aws:s3:::YOUR-BUCKET/deploy/*"] },
    { "Effect": "Allow", "Action": ["ssm:GetParametersByPath", "ssm:GetParameters"],
      "Resource": "arn:aws:ssm:us-east-1:*:parameter/nsi/stage*" },
    { "Effect": "Allow", "Action": "kms:Decrypt", "Resource": "*",
      "Condition": { "StringEquals": { "kms:ViaService": "ssm.us-east-1.amazonaws.com" } } }
  ]
}
```

## 3. The STAGE server (EC2)
**EC2 → Launch instance**
- **Name:** `nsi-stage` (the workflow finds the server by this name)
- **AMI:** Amazon Linux 2023 · **Instance type:** t3.micro
- **Key pair:** *Proceed without a key pair* (deploys use Systems Manager, not SSH)
- **Network settings → Edit:** create security group `nsi-stage-web`; remove the SSH rule; add rule **HTTP, port 80, Source: My IP**
- **Advanced details → IAM instance profile:** `nsi-stage-ec2`
- **Advanced details → User data:** paste
  ```
  #!/bin/bash
  dnf install -y docker
  systemctl enable --now docker
  mkdir -p /opt/nsi
  ```
- **Launch instance.** After ~3 minutes, **Systems Manager → Fleet Manager** should list `nsi-stage` as *Online*.
- Copy the instance's **Public IPv4 address** – STAGE will be at `http://<that address>`.

## 4. The STAGE database (RDS)
**RDS → Create database**
- **Standard create** · Engine **PostgreSQL** (version 16.x) · Template **Free tier** (or Dev/Test)
- **DB instance identifier:** `nsi-stage-db` · **Master username:** `nsi_app` ·
  **Credentials management:** *Self managed* · **Master password:** the `DB_PASSWORD` from step 1
- **Instance class:** db.t4g.micro · **Storage:** 20 GiB gp3, autoscaling off
- **Connectivity:** *Connect to an EC2 compute resource* → choose `nsi-stage` (AWS creates the security-group rules so only the server can reach the database) · **Public access:** No
- **Additional configuration → Initial database name:** `nsi_inventory` · Backup retention 1 day
- **Create database** (takes 5–10 minutes).

## 5. Point STAGE at the database
Open `nsi-stage-db` → **Connectivity & security** → copy the **Endpoint**
(like `nsi-stage-db.abc123xyz.us-east-1.rds.amazonaws.com`). In Parameter Store, edit `/nsi/stage/DB_HOST` and paste it.

## 6. Let GitHub deploy (IAM user `nsi-github-ci`)
**IAM → Users → nsi-github-ci → Add permissions → Create inline policy → JSON**, name it `nsi-ci-stage-deploy`:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Effect": "Allow", "Action": "s3:PutObject", "Resource": "arn:aws:s3:::YOUR-BUCKET/deploy/*" },
    { "Effect": "Allow", "Action": "ec2:DescribeInstances", "Resource": "*" },
    { "Effect": "Allow", "Action": "ssm:SendCommand",
      "Resource": ["arn:aws:ec2:us-east-1:*:instance/*", "arn:aws:ssm:us-east-1::document/AWS-RunShellScript"] },
    { "Effect": "Allow", "Action": ["ssm:GetCommandInvocation", "ssm:ListCommandInvocations"], "Resource": "*" }
  ]
}
```

## 7. GitHub settings
Repo → **Settings → Environments → New environment** `stage`:
- Variable `STAGE_URL` = `http://<EC2 public IPv4>`
- Optional: **Deployment branches** → *Selected branches* → `main` only

## 8. Deploy and check
**Actions → Deploy to STAGE → Run workflow** (build: `latest`). The log ends with four `PASS` lines and
`STAGE is running 2.0.0-xxxxxxx`. Open `http://<EC2 public IPv4>` and log in as `admin` with the
`NSI_BOOTSTRAP_ADMIN_PASSWORD` from step 1, then run the sign-off checklist in the Version 2 report.

## Troubleshooting
| Symptom | Fix |
|---|---|
| `No running EC2 instance named nsi-stage` | Instance stopped, or its Name tag is different |
| Deploy step hangs, then `Undeliverable`/`InvalidInstanceId` | Server not *Online* in Fleet Manager: check the IAM role from step 2 is attached |
| `Missing SSM parameter /nsi/stage/DB_HOST` | Step 1 or 5 not done; names are case-sensitive |
| `FAIL health not UP`, log mentions `Connection refused`/`timeout` | Database not reachable: redo the *Connect to an EC2 compute resource* option on the database |
| Browser can't open STAGE | Your IP changed: edit security group `nsi-stage-web`, set the HTTP rule source to *My IP* again |
