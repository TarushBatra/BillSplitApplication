# 🌐 Domain Setup Guide - Production Email Configuration

> **Note:** This guide is **optional**. For personal projects, you can use `onboarding@resend.dev` without any domain setup. See `PERSONAL_PROJECT_SETUP.md` for the simpler setup.

This guide will help you set up a custom domain for professional email sending with Resend (recommended for production/public apps).

---

## 📋 Table of Contents

1. [Why Use a Custom Domain?](#why-use-a-custom-domain)
2. [Domain Suggestions](#domain-suggestions)
3. [Step-by-Step Domain Setup](#step-by-step-domain-setup)
4. [Resend Domain Verification](#resend-domain-verification)
5. [Update Render Configuration](#update-render-configuration)
6. [Testing](#testing)
7. [Troubleshooting](#troubleshooting)

---

## 🎯 Why Use a Custom Domain?

### Current Setup (Test Domain)
- **From Email:** `onboarding@resend.dev`
- **Issues:**
  - ❌ Emails may go to spam
  - ❌ Not professional looking
  - ❌ Limited to testing

### Production Setup (Custom Domain)
- **From Email:** `noreply@yourdomain.com` or `hello@yourdomain.com`
- **Benefits:**
  - ✅ Better deliverability (less spam)
  - ✅ Professional appearance
  - ✅ Brand recognition
  - ✅ Higher trust from recipients

---

## 💡 Domain Suggestions

### Best Domain Extensions for BillSplit

#### Option 1: `.com` (Most Professional)
- `billsplit.com` ⭐ **Best choice**
- `splitbills.com`
- `billsplitter.com`
- `splitwise.com` (taken, but similar style)

#### Option 2: `.app` (Modern)
- `billsplit.app`
- `splitbills.app`

#### Option 3: `.io` (Tech-Friendly)
- `billsplit.io`
- `splitbills.io`

#### Option 4: `.co` (Short & Modern)
- `billsplit.co`
- `splitbills.co`

### Where to Buy Domains

1. **Namecheap** (Recommended)
   - URL: [namecheap.com](https://www.namecheap.com)
   - Price: ~$10-15/year for .com
   - Easy DNS management

2. **Google Domains**
   - URL: [domains.google](https://domains.google)
   - Price: ~$12/year for .com
   - Simple interface

3. **Cloudflare Registrar**
   - URL: [cloudflare.com/products/registrar](https://www.cloudflare.com/products/registrar)
   - Price: At-cost pricing (~$8-10/year)
   - Best for advanced users

4. **GoDaddy**
   - URL: [godaddy.com](https://www.godaddy.com)
   - Price: ~$12-15/year
   - Popular but can be expensive

### My Recommendation

**Best Choice:** `billsplit.com` or `billsplit.app`
- **Buy from:** Namecheap or Google Domains
- **Cost:** ~$10-15/year
- **Why:** Professional, memorable, easy to spell

---

## 📝 Step-by-Step Domain Setup

### Step 1: Purchase Your Domain

1. Go to your chosen registrar (e.g., Namecheap)
2. Search for your desired domain (e.g., `billsplit.com`)
3. Add to cart and complete purchase
4. Wait for domain activation (usually instant, max 24 hours)

### Step 2: Access Domain DNS Settings

1. Log into your domain registrar account
2. Find your domain in the dashboard
3. Click on "DNS Management" or "DNS Settings"
4. You'll see a list of DNS records

### Step 3: Add Domain to Resend

1. Go to [Resend Dashboard](https://resend.com/domains)
2. Click **"Add Domain"**
3. Enter your domain (e.g., `billsplit.com`)
4. Click **"Add"**
5. Resend will show you **3 DNS records** to add:
   - **SPF Record** (TXT)
   - **DKIM Record** (TXT)
   - **DMARC Record** (TXT) - Optional but recommended

### Step 4: Add DNS Records to Your Domain

In your domain registrar's DNS settings, add these records:

#### SPF Record (Required)
- **Type:** `TXT`
- **Name/Host:** `@` (or root domain)
- **Value:** Copy from Resend (looks like: `v=spf1 include:_spf.resend.com ~all`)
- **TTL:** `3600` (or default)

#### DKIM Record (Required)
- **Type:** `TXT`
- **Name/Host:** `resend._domainkey` (or similar, Resend will tell you)
- **Value:** Copy from Resend (long string starting with `v=DKIM1...`)
- **TTL:** `3600` (or default)

#### DMARC Record (Recommended)
- **Type:** `TXT`
- **Name/Host:** `_dmarc`
- **Value:** `v=DMARC1; p=none; rua=mailto:dmarc@yourdomain.com`
- **TTL:** `3600` (or default)

**Important:** 
- DNS changes can take 5 minutes to 48 hours to propagate
- Usually takes 10-30 minutes
- Use [whatsmydns.net](https://www.whatsmydns.net) to check propagation

### Step 5: Verify Domain in Resend

1. Go back to Resend Dashboard → Domains
2. Click on your domain
3. Click **"Verify Domain"**
4. Resend will check DNS records
5. Status will change to **"Verified"** ✅ (green)

**If verification fails:**
- Wait 10-15 minutes (DNS propagation)
- Double-check DNS records are correct
- Make sure TTL is set correctly
- Check for typos in record values

---

## 🔧 Update Render Configuration

Once your domain is verified in Resend:

### Step 1: Update Environment Variables in Render

1. Go to **Render Dashboard** → Backend Service → **"Environment"** tab

2. **Update `RESEND_FROM_EMAIL`:**
   - **Old:** `onboarding@resend.dev`
   - **New:** `noreply@yourdomain.com` (or `hello@yourdomain.com`)

3. **Add `RESEND_REPLY_TO` (Optional but Recommended):**
   - **Key:** `RESEND_REPLY_TO`
   - **Value:** `support@yourdomain.com` (or your support email)

4. **Update `APP_NAME` (Optional):**
   - **Key:** `APP_NAME`
   - **Value:** `BillSplit` (or your app name)

5. **Click "Save Changes"**
6. Render will automatically redeploy (2-3 minutes)

### Step 2: Verify Configuration

1. After redeploy, test: `https://[your-backend-url].onrender.com/api/health`
2. Should show:
   ```json
   {
     "email_configured": "true",
     "resend_from_email_set": "YES"
   }
   ```

---

## ✅ Testing

### Test Email Sending

1. **Test endpoint:**
   ```
   https://[your-backend-url].onrender.com/api/health/test-email?to=your-email@gmail.com
   ```

2. **Check your inbox:**
   - Email should arrive within 10-30 seconds
   - **From:** Should show `noreply@yourdomain.com` (not `onboarding@resend.dev`)
   - **Subject:** Should be formatted nicely
   - **Content:** Should be HTML formatted with your branding

3. **Check spam folder** (just in case, but should go to inbox)

### Test in Your App

1. **Invite a member** in BillSplit
2. **Check their inbox** - should receive professional email
3. **Verify email looks good:**
   - Professional HTML formatting
   - Correct branding
   - Working links

---

## 🚨 Troubleshooting

### Issue: Domain verification fails

**Solutions:**
1. **Wait longer:** DNS can take up to 48 hours (usually 10-30 min)
2. **Check DNS records:** Use [whatsmydns.net](https://www.whatsmydns.net) to verify records are propagated
3. **Verify record format:** Make sure you copied the exact values from Resend
4. **Check TTL:** Should be 3600 or lower
5. **Remove old records:** If you had previous SPF/DKIM records, remove them first

### Issue: Emails still using test domain

**Solutions:**
1. **Check `RESEND_FROM_EMAIL`** in Render environment variables
2. **Verify it's updated** to `noreply@yourdomain.com`
3. **Wait for redeploy** to complete
4. **Check logs** in Render to see what email address is being used

### Issue: Emails going to spam

**Solutions:**
1. **Use custom domain** (not test domain)
2. **Set up DMARC record** (helps with deliverability)
3. **Warm up your domain:** Start with low volume, gradually increase
4. **Monitor Resend dashboard:** Check delivery rates and spam reports

### Issue: DNS records not showing up

**Solutions:**
1. **Check DNS propagation:** [whatsmydns.net](https://www.whatsmydns.net)
2. **Clear DNS cache:** 
   - Windows: `ipconfig /flushdns`
   - Mac/Linux: `sudo dscacheutil -flushcache`
3. **Try different DNS server:** Use Google DNS (8.8.8.8) or Cloudflare (1.1.1.1)
4. **Contact registrar support:** If records still don't show after 24 hours

---

## 📊 Email Address Recommendations

### For Different Purposes

| Purpose | Email Format | Example |
|---------|-------------|---------|
| **System/Notifications** | `noreply@yourdomain.com` | `noreply@billsplit.com` |
| **Support** | `support@yourdomain.com` | `support@billsplit.com` |
| **General Contact** | `hello@yourdomain.com` | `hello@billsplit.com` |
| **Team** | `team@yourdomain.com` | `team@billsplit.com` |

### Recommended Setup

- **`RESEND_FROM_EMAIL`:** `noreply@yourdomain.com`
  - Used for all automated emails (invitations, notifications)
  - Users know not to reply

- **`RESEND_REPLY_TO`:** `support@yourdomain.com`
  - If users click "Reply", emails go here
  - You can set up a support email inbox

---

## 🎉 Success Checklist

- [ ] Domain purchased
- [ ] DNS records added to domain registrar
- [ ] Domain verified in Resend dashboard
- [ ] `RESEND_FROM_EMAIL` updated in Render
- [ ] `RESEND_REPLY_TO` added to Render (optional)
- [ ] Backend redeployed
- [ ] Test email sent successfully
- [ ] Email shows custom domain (not `onboarding@resend.dev`)
- [ ] Email arrives in inbox (not spam)
- [ ] HTML formatting looks good

---

## 💰 Cost Summary

- **Domain:** ~$10-15/year (.com)
- **Resend:** FREE (3,000 emails/month)
- **Total:** ~$10-15/year

**Very affordable for a professional setup!**

---

## 📚 Additional Resources

- **Resend Domain Docs:** [resend.com/docs/dashboard/domains/introduction](https://resend.com/docs/dashboard/domains/introduction)
- **DNS Propagation Checker:** [whatsmydns.net](https://www.whatsmydns.net)
- **SPF Record Generator:** [spfrecord.com](https://www.spfrecord.com)
- **DMARC Guide:** [dmarc.org](https://dmarc.org)

---

## 🎯 Quick Start Summary

1. **Buy domain:** `billsplit.com` from Namecheap (~$10/year)
2. **Add domain to Resend:** Dashboard → Domains → Add Domain
3. **Add DNS records:** Copy SPF, DKIM, DMARC from Resend to your registrar
4. **Verify in Resend:** Wait 10-30 minutes, then verify
5. **Update Render:** Change `RESEND_FROM_EMAIL` to `noreply@yourdomain.com`
6. **Test:** Send test email and verify it works!

**Total time: ~30-60 minutes (mostly waiting for DNS propagation)**

---

**Questions?** Check Resend docs or test with the `/api/health/test-email` endpoint!

**Happy Domain Setup! 🌐✨**
