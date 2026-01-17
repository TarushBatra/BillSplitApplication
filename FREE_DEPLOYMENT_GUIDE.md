# 🚀 Free Deployment Guide - Step by Step

This guide will help you deploy BillSplit **completely FREE** with full functionality.

## 🎯 Best Free Hosting Options

### Option 1: Railway (Recommended - Easiest)
- **Free Tier:** $5 credit/month (usually enough for small apps)
- **PostgreSQL:** Included free
- **SSL/HTTPS:** Automatic
- **Deployment:** Very easy, connects to GitHub
- **Best for:** Quick deployment with minimal setup

### Option 2: Render
- **Free Tier:** Available with limitations
- **PostgreSQL:** Free tier available
- **SSL/HTTPS:** Automatic
- **Limitations:** Services sleep after 15 min inactivity (free tier)
- **Best for:** Long-term free hosting

### Option 3: Fly.io
- **Free Tier:** 3 shared VMs
- **PostgreSQL:** Available
- **SSL/HTTPS:** Automatic
- **Best for:** More control, slightly more complex

**We'll use Railway for this guide (easiest option)**

---

## 📋 STEP-BY-STEP DEPLOYMENT

### STEP 1: Prepare Your Code (GitHub)

#### 1.1 Create GitHub Repository

1. Go to [GitHub.com](https://github.com) and sign in
2. Click the **"+"** icon in the top right → **"New repository"**
3. Repository name: `BillSplitApplication` (or any name)
4. Set to **Public** (for free hosting)
5. **DO NOT** initialize with README (you already have one)
6. Click **"Create repository"**

#### 1.2 Push Your Code to GitHub

Open your terminal/PowerShell in your project folder and run:

```bash
# Initialize git (if not already done)
git init

# Add all files
git add .

# Commit
git commit -m "Initial commit - Production ready"

# Add your GitHub repository (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/BillSplitApplication.git

# Push to GitHub
git branch -M main
git push -u origin main
```

**Note:** If you get authentication errors, you may need to:
- Use GitHub Personal Access Token instead of password
- Or use GitHub Desktop app

---

### STEP 2: Generate Required Secrets

Before deploying, generate your secrets:

#### 2.1 Generate JWT Secret

**On Windows (PowerShell):**
```powershell
# Option 1: Using OpenSSL (if installed)
openssl rand -base64 32

# Option 2: Using Node.js (if you have Node.js)
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"

# Option 3: Using Python (if you have Python)
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

**Copy the generated secret** - you'll need it later!

#### 2.2 Generate Database Password

Create a strong password (at least 16 characters):
- Use a password generator, or
- Use: `openssl rand -base64 16` (if available)

**Copy this password** - you'll need it later!

---

### STEP 3: Deploy to Railway

#### 3.1 Sign Up for Railway

1. Go to [railway.app](https://railway.app)
2. Click **"Start a New Project"**
3. Sign up with **GitHub** (easiest option)
4. Authorize Railway to access your GitHub

#### 3.2 Create New Project

1. Click **"New Project"**
2. Select **"Deploy from GitHub repo"**
3. Choose your `BillSplitApplication` repository
4. Railway will detect it's a monorepo

#### 3.3 Add PostgreSQL Database

1. In your Railway project, click **"+ New"**
2. Select **"Database"** → **"Add PostgreSQL"**
3. Railway will create a PostgreSQL database
4. **IMPORTANT:** Click on the PostgreSQL service
5. Go to **"Variables"** tab
6. **Copy these values** (you'll need them):
   - `PGHOST` (hostname)
   - `PGPORT` (port, usually 5432)
   - `PGDATABASE` (database name)
   - `PGUSER` (username)
   - `PGPASSWORD` (password)

#### 3.4 Deploy Backend Service

1. In Railway project, click **"+ New"**
2. Select **"GitHub Repo"** → Choose your repo
3. Railway will ask which service to deploy
4. Select **"backend"** directory
5. Railway will automatically:
   - Detect it's a Spring Boot app
   - Build the Docker image
   - Deploy it

6. **Configure Backend Environment Variables:**
   - Click on the backend service
   - Go to **"Variables"** tab
   - Click **"+ New Variable"**
   - Add each of these variables:

```
SPRING_DATASOURCE_URL = jdbc:postgresql://PGHOST:PGPORT/PGDATABASE
SPRING_DATASOURCE_USERNAME = PGUSER
SPRING_DATASOURCE_PASSWORD = PGPASSWORD
JWT_SECRET = [Your generated JWT secret from Step 2.1]
APP_URL = https://[your-backend-domain].railway.app
CORS_ALLOWED_ORIGINS = https://[your-frontend-domain].railway.app
LOG_LEVEL = INFO
SECURITY_LOG_LEVEL = WARN
RATE_LIMIT_ENABLED = true
RATE_LIMIT_REQUESTS = 100
RATE_LIMIT_WINDOW_MINUTES = 1
MAIL_USERNAME = [Your email - optional]
MAIL_PASSWORD = [Your email app password - optional]
```

**Important Notes:**
- Replace `PGHOST`, `PGPORT`, etc. with actual values from Step 3.3
- Replace `[your-backend-domain]` with Railway's generated domain (shown in Settings → Domains)
- Replace `[your-frontend-domain]` with your frontend domain (we'll get this in next step)

7. **Get Backend URL:**
   - Go to backend service → **"Settings"** tab
   - Scroll to **"Domains"**
   - Railway will generate a domain like: `your-app-production.up.railway.app`
   - **Copy this URL** - this is your backend API URL

#### 3.5 Deploy Frontend Service

1. In Railway project, click **"+ New"**
2. Select **"GitHub Repo"** → Choose your repo
3. Select **"frontend"** directory
4. Railway will detect it's a React app

5. **Configure Frontend Environment Variables:**
   - Click on frontend service
   - Go to **"Variables"** tab
   - Add:
   ```
   REACT_APP_API_URL = https://[your-backend-domain].railway.app/api
   ```
   - Replace `[your-backend-domain]` with your backend domain from Step 3.4

6. **Update Backend CORS:**
   - Go back to backend service → **"Variables"**
   - Update `CORS_ALLOWED_ORIGINS` with your frontend domain:
   ```
   CORS_ALLOWED_ORIGINS = https://[your-frontend-domain].railway.app
   ```

7. **Get Frontend URL:**
   - Go to frontend service → **"Settings"** → **"Domains"**
   - Railway will generate a domain
   - **Copy this URL** - this is your app URL

#### 3.6 Update Environment Variables with Final URLs

Go back to backend service → **"Variables"** and update:

```
APP_URL = https://[your-frontend-domain].railway.app
CORS_ALLOWED_ORIGINS = https://[your-frontend-domain].railway.app
```

---

### STEP 4: Configure Email (Optional but Recommended)

#### 4.1 Gmail Setup (Free)

1. Go to your Google Account → **Security**
2. Enable **2-Step Verification** (required for App Passwords)
3. Go to **App Passwords**
4. Generate a new app password for "Mail"
5. **Copy the 16-character password**

#### 4.2 Add Email to Railway

1. Go to backend service → **"Variables"**
2. Add:
   ```
   MAIL_USERNAME = your-email@gmail.com
   MAIL_PASSWORD = [16-character app password from Step 4.1]
   ```

---

### STEP 5: Test Your Deployment

#### 5.1 Check Backend Health

Open in browser:
```
https://[your-backend-domain].railway.app/api/health
```

Should return:
```json
{"status":"UP","service":"BillSplit Backend"}
```

#### 5.2 Check Frontend

Open in browser:
```
https://[your-frontend-domain].railway.app
```

Should show your app!

#### 5.3 Test Features

1. **Register a new user**
2. **Login**
3. **Create a group**
4. **Add an expense**
5. **Test settlement**

---

## 🔄 Alternative: Render Deployment

If Railway doesn't work or you prefer Render:

### Render Setup Steps

1. **Sign up at [render.com](https://render.com)** (free tier available)

2. **Create PostgreSQL Database:**
   - Dashboard → **"New +"** → **"PostgreSQL"**
   - Name: `billsplit-db`
   - Plan: **Free**
   - Click **"Create Database"**
   - **Copy connection string** from dashboard

3. **Deploy Backend:**
   - Dashboard → **"New +"** → **"Web Service"**
   - Connect your GitHub repo
   - Root Directory: `backend`
   - Build Command: `./mvnw clean package -DskipTests` (or `mvn clean package -DskipTests`)
   - Start Command: `java -jar target/bill-split-backend-0.0.1-SNAPSHOT.jar`
   - Environment: **Docker** (if using Dockerfile) or **Java**
   - Add all environment variables (same as Railway)

4. **Deploy Frontend:**
   - Dashboard → **"New +"** → **"Static Site"**
   - Connect your GitHub repo
   - Root Directory: `frontend`
   - Build Command: `npm install && npm run build`
   - Publish Directory: `build`
   - Add environment variable: `REACT_APP_API_URL`

---

## 🚨 Troubleshooting

### Issue: Backend won't start
- Check logs in Railway/Render dashboard
- Verify all environment variables are set
- Check database connection string

### Issue: CORS errors
- Verify `CORS_ALLOWED_ORIGINS` matches your frontend URL exactly
- Check for trailing slashes
- Ensure URLs use `https://` not `http://`

### Issue: Database connection fails
- Verify database credentials
- Check `SPRING_DATASOURCE_URL` format
- Ensure database is running

### Issue: Frontend can't connect to backend
- Verify `REACT_APP_API_URL` is correct
- Check backend is running
- Test backend health endpoint

---

## ✅ Deployment Checklist

- [ ] Code pushed to GitHub
- [ ] Railway/Render account created
- [ ] PostgreSQL database created
- [ ] Backend service deployed
- [ ] Frontend service deployed
- [ ] All environment variables set
- [ ] JWT secret generated and set
- [ ] Database credentials configured
- [ ] CORS origins set correctly
- [ ] Email configured (optional)
- [ ] Health endpoint working
- [ ] Frontend accessible
- [ ] All features tested

---

## 🎉 You're Done!

Your app should now be live and accessible at:
- **Frontend:** `https://[your-frontend-domain].railway.app`
- **Backend API:** `https://[your-backend-domain].railway.app/api`

**Congratulations! Your BillSplit app is now deployed! 🚀**
