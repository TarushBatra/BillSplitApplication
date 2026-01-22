# 📧 SendGrid Setup Guide - FREE Email Service

**Perfect for personal projects!** SendGrid allows sending to ANY email address without domain verification on the free tier.

---

## ✅ Why SendGrid?

- ✅ **100 emails/day FREE** (3,000/month)
- ✅ **No domain required** - can send to any email address
- ✅ **No credit card required**
- ✅ **HTTP API** (works on Render)
- ✅ **Great deliverability**

---

## 📝 STEP 1: Create SendGrid Account

1. Go to **[sendgrid.com](https://sendgrid.com)**
2. Click **"Start for Free"** or **"Sign Up"**
3. Fill in:
   - **Email:** Your email address
   - **Password:** Create a password
   - **Company Name:** Your name or "Personal Project" (optional)
4. Click **"Create Account"**
5. Verify your email (check inbox)
6. Complete the onboarding (skip optional steps if you want)

---

## 🔑 STEP 2: Create API Key

1. In SendGrid dashboard, go to **"Settings"** → **"API Keys"** (left sidebar)
2. Click **"Create API Key"** button
3. Fill in:
   - **API Key Name:** `BillSplit Production` (or any name)
   - **API Key Permissions:** Select **"Full Access"** (or "Restricted Access" with "Mail Send" permission)
4. Click **"Create & View"**
5. **IMPORTANT:** Copy the API key immediately!
   - It looks like: `SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`
   - **You can only see it once!** If you lose it, create a new one.
6. **Save this API key** - you'll need it for Render

---

## 📧 STEP 3: Verify Sender Identity (Required)

SendGrid requires you to verify a sender email address:

### Option A: Single Sender Verification (Easiest - Recommended)

1. Go to **"Settings"** → **"Sender Authentication"** → **"Single Sender Verification"**
2. Click **"Create New Sender"**
3. Fill in the form:
   - **From Email Address:** Your email (e.g., `tarushbatra11318@gmail.com`)
   - **From Name:** `BillSplit` (or your app name)
   - **Reply To:** Same as from email (or leave default)
   - **Address:** Your address (optional)
   - **City, State, Zip:** Your location (optional)
4. Click **"Create"**
5. **Check your email inbox** for verification email from SendGrid
6. **Click the verification link** in the email
7. Status will change to **"Verified"** ✅

**Note:** You can only send FROM this verified email address, but you can send TO any email address!

---

## 🔧 STEP 4: Configure Render Environment Variables

1. Go to **Render Dashboard** → Backend Service (`billsplit-backend`)
2. Click **"Environment"** tab
3. Add/Update these variables:

### Add SENDGRID_API_KEY:

- **Key:** `SENDGRID_API_KEY`
- **Value:** Your API key from Step 2 (starts with `SG.`)
- Click **"Save Changes"**

### Add SENDGRID_FROM_EMAIL:

- **Key:** `SENDGRID_FROM_EMAIL`
- **Value:** Your verified email from Step 3 (e.g., `tarushbatra11318@gmail.com`)
- Click **"Save Changes"**

### Add SENDGRID_FROM_NAME (Optional):

- **Key:** `SENDGRID_FROM_NAME`
- **Value:** `BillSplit` (or your app name)
- Click **"Save Changes"**

### Remove Old Resend Variables (Optional):

You can remove these (they're no longer used):
- `RESEND_API_KEY`
- `RESEND_FROM_EMAIL`
- `RESEND_REPLY_TO`

---

## 🚀 STEP 5: Deploy and Test

1. **Push your code to GitHub:**
   ```bash
   git push origin main
   ```

2. **Wait for Render to redeploy** (2-3 minutes)

3. **Test email configuration:**
   - Open: `https://[your-backend-url].onrender.com/api/health`
   - You should see:
     ```json
     {
       "status": "UP",
       "email_configured": "true",
       "sendgrid_api_key_set": "YES",
       "sendgrid_from_email_set": "YES",
       "email_provider": "SendGrid API (Free: 100 emails/day)"
     }
     ```

4. **Test sending an email:**
   - Open: `https://[your-backend-url].onrender.com/api/health/test-email?to=sanjeevsonia3@gmail.com`
   - Should return: `{"status": "success", ...}`
   - **Check inbox!** Email should arrive within seconds! 🎉

---

## ✅ STEP 6: Verify It Works

1. **Send a test email** using the test endpoint
2. **Check your inbox** - email should arrive within 10-30 seconds
3. **Try inviting a member** in your BillSplit app
4. **Check if they receive the invitation email**

---

## 🎉 Success Checklist

- [ ] SendGrid account created
- [ ] API key generated and copied
- [ ] Sender email verified
- [ ] `SENDGRID_API_KEY` set in Render
- [ ] `SENDGRID_FROM_EMAIL` set in Render (your verified email)
- [ ] `SENDGRID_FROM_NAME` set in Render (optional)
- [ ] Code pushed to GitHub
- [ ] Render redeployed
- [ ] `/api/health` shows `email_configured: true`
- [ ] Test email sent successfully
- [ ] Email received in inbox! ✅
- [ ] Can invite members and they receive emails! ✅

---

## 🚨 Troubleshooting

### Issue: `email_configured: false`

**Solution:**
- Check `SENDGRID_API_KEY` is set correctly in Render
- Check `SENDGRID_FROM_EMAIL` is set correctly
- Make sure there are no extra spaces in the values

### Issue: Test email returns error

**Check Render logs:**
1. Go to Render Dashboard → Backend Service → "Logs" tab
2. Look for error messages
3. Common errors:
   - `401 Unauthorized` → API key is wrong
   - `403 Forbidden` → Sender email not verified
   - `400 Bad Request` → Invalid email format

### Issue: "Sender email not verified"

**Solution:**
1. Go to SendGrid → Settings → Sender Authentication
2. Check if your sender is verified
3. If not, check your email inbox for verification link
4. Click the verification link
5. Wait for status to change to "Verified"

### Issue: Email not received

**Solutions:**
1. Check spam folder
2. Wait 30-60 seconds (sometimes takes a moment)
3. Check SendGrid dashboard → Activity → Email Activity
4. See if email was sent and delivery status

### Issue: Rate limiting

**Solution:**
- Free tier: 100 emails/day
- If you hit the limit, wait until next day or upgrade

---

## 📊 SendGrid Dashboard

You can monitor your emails in SendGrid:

1. Go to SendGrid dashboard
2. Click **"Activity"** in the left sidebar
3. See all sent emails, delivery status, opens, bounces, etc.
4. Very useful for debugging!

---

## 💰 Cost

- **Free Tier:** 100 emails/day (3,000/month)
- **Paid Plans:** Start at $19.95/month (if you need more)

**For your BillSplit app, the free tier is perfect!**

---

## 🎯 Key Differences from Resend

| Feature | Resend (Test Domain) | SendGrid (Free) |
|---------|----------------------|-----------------|
| **Can send to any email?** | ❌ Only your email | ✅ Yes! |
| **Domain required?** | ❌ No (but limited) | ❌ No |
| **Free tier** | 3,000/month | 100/day (3,000/month) |
| **Sender verification** | ❌ Not needed | ✅ Required (easy) |

---

## 🎉 Congratulations!

Your email functionality is now working with SendGrid! You can send invitations to ANY email address without needing a custom domain.

**Questions?** Check SendGrid docs: [sendgrid.com/docs](https://docs.sendgrid.com)

---

**Happy Email Sending! 📧✨**
