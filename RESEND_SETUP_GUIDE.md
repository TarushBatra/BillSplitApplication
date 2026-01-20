# 📧 Resend Email Setup Guide - Step by Step

This guide will help you set up **Resend** (free email API) to replace Gmail SMTP, which doesn't work on Render's free tier.

## ✅ Why Resend?

- ✅ **3,000 emails/month FREE** (plenty for your app!)
- ✅ **No credit card required**
- ✅ **HTTP API** (works on Render - no SMTP blocking!)
- ✅ **Great deliverability** (emails actually arrive!)
- ✅ **Easy setup** (5 minutes)

---

## 📝 STEP 1: Create Resend Account

1. Go to **[resend.com](https://resend.com)**
2. Click **"Sign Up"** (top right)
3. Sign up with:
   - **Email:** Your email address
   - **Password:** Create a password
   - Or use **"Continue with Google"** (easiest)
4. Verify your email (check inbox for verification link)
5. You'll be redirected to the Resend dashboard

---

## 🔑 STEP 2: Get Your API Key

1. In Resend dashboard, click **"API Keys"** in the left sidebar
2. Click **"Create API Key"** button
3. Fill in:
   - **Name:** `BillSplit Production` (or any name you like)
   - **Permission:** Select **"Sending access"** (default)
4. Click **"Add"**
5. **IMPORTANT:** Copy the API key immediately!
   - It looks like: `re_123456789abcdefghijklmnopqrstuvwxyz`
   - **You can only see it once!** If you lose it, create a new one.
6. **Save this API key** - you'll need it for Render

---

## 📧 STEP 3: Add Your Domain (Optional but Recommended)

### Option A: Use Resend's Test Domain (Quick Start)

For testing, Resend provides a test domain. You can skip domain verification for now and use:
- **From Email:** `onboarding@resend.dev` (for testing only)

**Note:** Test domain emails may go to spam. For production, add your own domain.

### Option B: Add Your Own Domain (Production)

1. In Resend dashboard, click **"Domains"** in the left sidebar
2. Click **"Add Domain"**
3. Enter your domain (e.g., `billsplit.com` or `yourdomain.com`)
4. Follow Resend's instructions to add DNS records:
   - Add **SPF record**
   - Add **DKIM record**
   - Add **DMARC record** (optional but recommended)
5. Wait for verification (usually 5-10 minutes)
6. Once verified, you can use: `noreply@yourdomain.com` as your from address

**For now, use Option A (test domain) to get started quickly!**

---

## 🔧 STEP 4: Configure Render Environment Variables

1. Go to **Render Dashboard** → Your Backend Service (`billsplit-backend`)
2. Click **"Environment"** tab
3. Add/Update these environment variables:

### Add RESEND_API_KEY:

- **Key:** `RESEND_API_KEY`
- **Value:** Your API key from Step 2 (e.g., `re_123456789abcdefghijklmnopqrstuvwxyz`)
- Click **"Save Changes"**

### Add RESEND_FROM_EMAIL:

- **Key:** `RESEND_FROM_EMAIL`
- **Value:** 
  - For testing: `onboarding@resend.dev`
  - For production: `noreply@yourdomain.com` (after domain verification)
- Click **"Save Changes"**

### Remove Old Gmail Variables (Optional):

You can remove these if you want (they're no longer used):
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

**Don't delete them yet** - wait until Resend is working!

---

## 🚀 STEP 5: Deploy and Test

1. **Push your code to GitHub:**
   ```bash
   git add .
   git commit -m "Switch from Gmail SMTP to Resend API"
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
       "resend_api_key_set": "YES",
       "resend_from_email_set": "YES",
       "email_provider": "Resend API"
     }
     ```

4. **Test sending an email:**
   - Open: `https://[your-backend-url].onrender.com/api/health/test-email?to=your-email@gmail.com`
   - Should return: `{"status": "success", ...}`
   - **Check your inbox!** Email should arrive within seconds! 🎉

---

## ✅ STEP 6: Verify It Works

1. **Send a test email** using the test endpoint
2. **Check your inbox** - email should arrive within 10-30 seconds
3. **Check spam folder** (just in case, but Resend has good deliverability)
4. **Try inviting a member** in your BillSplit app
5. **Check if they receive the invitation email**

---

## 🎉 Success Checklist

- [ ] Resend account created
- [ ] API key generated and copied
- [ ] `RESEND_API_KEY` set in Render
- [ ] `RESEND_FROM_EMAIL` set in Render (using `onboarding@resend.dev` for testing)
- [ ] Code pushed to GitHub
- [ ] Render redeployed
- [ ] `/api/health` shows `email_configured: true`
- [ ] Test email sent successfully
- [ ] Email received in inbox! ✅

---

## 🚨 Troubleshooting

### Issue: `email_configured: false`

**Solution:**
- Check `RESEND_API_KEY` is set correctly in Render
- Check `RESEND_FROM_EMAIL` is set correctly
- Make sure there are no extra spaces in the values

### Issue: Test email returns error

**Check Render logs:**
1. Go to Render Dashboard → Backend Service → "Logs" tab
2. Look for error messages
3. Common errors:
   - `401 Unauthorized` → API key is wrong
   - `422 Unprocessable Entity` → From email is invalid
   - `403 Forbidden` → API key doesn't have sending permission

### Issue: Email not received

**Solutions:**
1. Check spam folder
2. Wait 30-60 seconds (sometimes takes a moment)
3. Check Resend dashboard → "Emails" tab to see if email was sent
4. If using test domain (`onboarding@resend.dev`), emails may go to spam

### Issue: Rate limiting

**Solution:**
- Free tier: 3,000 emails/month
- If you hit the limit, wait until next month or upgrade

---

## 📊 Resend Dashboard

You can monitor your emails in Resend:

1. Go to Resend dashboard
2. Click **"Emails"** in the left sidebar
3. See all sent emails, delivery status, opens, etc.
4. Very useful for debugging!

---

## 💰 Cost

- **Free Tier:** 3,000 emails/month
- **Paid Plans:** Start at $20/month (if you need more)

**For your BillSplit app, the free tier is more than enough!**

---

## 🎯 Next Steps

Once Resend is working:

1. ✅ Test all email features:
   - Group invitations
   - Expense notifications
   - Settlement notifications
   - Invitation rejections

2. ✅ (Optional) Add your own domain for better deliverability:
   - Follow Step 3 Option B
   - Update `RESEND_FROM_EMAIL` to use your domain

3. ✅ Remove old Gmail environment variables from Render (optional cleanup)

---

## 🎉 Congratulations!

Your email functionality is now working! Emails will actually be delivered, unlike with Gmail SMTP on Render.

**Questions?** Check Resend docs: [resend.com/docs](https://resend.com/docs)

---

**Happy Email Sending! 📧✨**
