# Environment Configuration

This project uses GitHub Environments for deployment protection and environment-specific configurations.

## Environments

### Prod
- **Purpose**: Deploy releases to Maven Central
- **Protection**: Requires manual approval from repository administrators
- **URL**: https://central.sonatype.com/artifact/dev.gmky/gmk-spring-utils

### Staging (Optional)
- **Purpose**: Test releases before prod deployment
- **Protection**: No approval required
- **URL**: Staging repository on Sonatype

## Required Secrets

Configure these secrets in your GitHub repository settings under **Settings → Secrets and variables → Actions**.

### Repository Secrets

| Secret Name | Description | Required For |
|------------|-------------|--------------|
| `OSSRH_USERNAME` | Set to the string: `token` | All deployments |
| `OSSRH_TOKEN` | Your Central Portal User Token | All deployments |
| `GPG_PRIVATE_KEY` | GPG private key (armored export) | All deployments |
| `GPG_PASSPHRASE` | GPG key passphrase | All deployments |

### Environment-Specific Secrets (Optional)

You can override repository secrets at the environment level for additional security:

**Prod Environment:**
- `OSSRH_USERNAME`
- `OSSRH_TOKEN`
- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`

## Setting Up GitHub Environments

### 1. Create Prod Environment

1. Go to **Settings → Environments** in your repository
2. Click **New environment**
3. Name it `prod`
4. Click **Configure environment**

### 2. Configure Environment Protection Rules

For the `prod` environment:

- ✅ **Required reviewers**: Add repository administrators
- ✅ **Wait timer**: Optional (e.g., 5 minutes delay before deployment)
- ✅ **Deployment branches**: Only allow `main` branch

### 3. Add Environment Secrets (Optional)

If you want environment-specific credentials:

1. In the environment configuration
2. Scroll to **Environment secrets**
3. Add the secrets listed above

## Deployment Workflows

### Automatic Deployment (On Release)

Triggered when you create a GitHub release:

```bash
# Create and push a tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Create a release on GitHub from the tag
# The publish.yml workflow will trigger automatically
```

### Manual Prod Deployment

Use the `Deploy to Prod` workflow for controlled deployments:

1. Go to **Actions → Deploy to Prod**
2. Click **Run workflow**
3. Enter the version number (e.g., `1.0.0`)
4. Type `deploy` to confirm
5. Click **Run workflow**
6. **Approve the deployment** when prompted (if protection rules are enabled)

### Manual Deployment with Environment Selection

Use the enhanced `Publish to Maven Central` workflow:

1. Go to **Actions → Publish to Maven Central**
2. Click **Run workflow**
3. Select environment: `prod` or `staging`
4. Click **Run workflow**

## Deployment Process

### Prod Deployment Flow

```
┌─────────────────────┐
│  Trigger Workflow   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Validate Inputs    │
│  - Confirmation     │
│  - Version format   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Wait for Approval  │◄── Repository Admin
│  (if configured)    │    Reviews & Approves
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Build & Test       │
│  - Compile code     │
│  - Run tests        │
│  - Generate JARs    │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Deploy to Maven    │
│  Central            │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Create Git Tag     │
│  Upload Artifacts   │
│  Generate Summary   │
└─────────────────────┘
```

## Best Practices

### 1. Version Management
- Use semantic versioning (MAJOR.MINOR.PATCH)
- Tag releases in Git
- Keep CHANGELOG.md updated

### 2. Security
- Rotate secrets periodically
- Use environment-specific secrets for prod
- Enable required reviewers for prod deployments

### 3. Deployment Checklist

Before deploying to prod:
- [ ] All tests pass
- [ ] Code review completed
- [ ] CHANGELOG.md updated
- [ ] Version number incremented
- [ ] Documentation updated

### 4. Monitoring

After deployment:
- [ ] Verify artifacts on Maven Central
- [ ] Check download/usage metrics
- [ ] Monitor for issues or bug reports
- [ ] Update release notes

## Troubleshooting

### Deployment Failed

1. Check the workflow logs for error details
2. Verify all secrets are correctly configured
3. Ensure GPG key is valid and passphrase is correct
4. Verify Sonatype credentials are active

### GPG Signing Issues

```bash
# Export your GPG key
gpg --armor --export-secret-keys YOUR_KEY_ID

# Test GPG signing locally
gpg --sign test.txt
```

### Maven Central Sync Delays

- It may take 15-30 minutes for artifacts to appear on Maven Central
- Check Sonatype OSSRH for staging repository status
- Verify automatic release is enabled in nexus-staging-maven-plugin

## Environment URLs

- **Maven Central Search**: https://central.sonatype.com/
- **Sonatype OSSRH**: https://s01.oss.sonatype.org/
- **Repository Admin**: https://s01.oss.sonatype.org/#stagingRepositories
