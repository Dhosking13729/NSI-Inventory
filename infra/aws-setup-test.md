# AWS setup for the TEST stage (Version 1)

In my design (Submission 3, Section 2) TEST is GitHub Actions: it builds every push, runs the unit and
integration tests against a test database, and saves each passing build to Amazon S3. So Version 1 needs
only **one S3 bucket** and **credentials that can write to it**. EC2 and RDS are set up for STAGE in Module 6.

## 1. Create the bucket (AWS Console)
1. S3 → **Create bucket**. Name: `nsi-inventory-builds-<your initials><4 digits>` (must be globally unique). Region: `us-east-1`.
2. Keep **Block all public access** ON. Turn **Bucket Versioning** ON. Create.

## 2. Credentials GitHub can use
**Regular AWS account**
1. IAM → Users → **Create user** `nsi-github-ci` (no console access).
2. Permissions → **Attach policies directly** → Create policy → JSON, paste this (put your bucket name in):
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       { "Effect": "Allow", "Action": ["s3:PutObject", "s3:GetObject"], "Resource": "arn:aws:s3:::YOUR-BUCKET/builds/*" },
       { "Effect": "Allow", "Action": "s3:ListBucket", "Resource": "arn:aws:s3:::YOUR-BUCKET" }
     ]
   }
   ```
   Name it `nsi-ci-s3-builds` and attach it to the user.
3. Open the user → **Security credentials** → **Create access key** → "Application running outside AWS".
   Copy the Access key ID and Secret access key.

**AWS Academy (Learner Lab)** – you can't create IAM users. Start the lab, click **AWS Details** → **Show** next
to AWS CLI, and use `aws_access_key_id`, `aws_secret_access_key` and `aws_session_token`. These expire when the
lab session ends, so update the three GitHub secrets before each run that needs S3.

## 3. GitHub settings (repo → Settings → Secrets and variables → Actions)
| Type | Name | Value |
|---|---|---|
| Secret | `AWS_ACCESS_KEY_ID` | from step 2 |
| Secret | `AWS_SECRET_ACCESS_KEY` | from step 2 |
| Secret | `AWS_SESSION_TOKEN` | AWS Academy only; leave it out otherwise |
| Variable | `AWS_REGION` | `us-east-1` |
| Variable | `ARTIFACT_BUCKET` | your bucket name |

## 4. Check it
Merge a pull request into `main`. In **Actions → CI - DEV to TEST**, the job **Save passing build to Amazon S3**
should be green, and the bucket should contain `builds/1.0.0-<commit>/nsi-inventory.jar`, the test and coverage
reports, `build-info.txt`, and `builds/latest.txt`.
