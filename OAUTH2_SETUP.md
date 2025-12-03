# OAuth2 Sign-In Guide

This backend now supports Google (and optional GitHub) OAuth2 login on top of the existing username/password + JWT flow.

## 1. Create OAuth Applications

### Google
1. Open [Google Cloud Console](https://console.cloud.google.com/apis/credentials).
2. Create OAuth Client (type **Web application**).
3. Authorized redirect URI: `http://localhost:9292/login/oauth2/code/google` (adjust host/port for prod).
4. Copy the client id/secret.

### GitHub (optional)
1. Go to [GitHub Developer Settings](https://github.com/settings/developers).
2. Create OAuth App with callback `http://localhost:9292/login/oauth2/code/github`.
3. Copy the client id/secret.

## 2. Configure Environment

Set the following properties (can be exported as env vars thanks to the placeholders in `application.properties`):

```
GOOGLE_OAUTH_CLIENT_ID=<client_id>
GOOGLE_OAUTH_CLIENT_SECRET=<client_secret>
GITHUB_OAUTH_CLIENT_ID=<client_id>
GITHUB_OAUTH_CLIENT_SECRET=<client_secret>
```

Alternatively, override the `spring.security.oauth2.client.registration.*` keys in the respective profile file.

## 3. Start the Authorization Code Flow

Redirect the user to one of Spring Security's built-in endpoints:
- `GET /oauth2/authorization/google`
- `GET /oauth2/authorization/github`

After the provider callback, the backend responds with a JSON payload identical to `/api/v1/auth/login`:

```json
{
  "token": "<jwt>",
  "user": {
    "id": 1,
    "name": "Alice",
    "email": "alice@example.com",
    "provider": "GOOGLE",
    "emailVerified": true,
    "roles": [...]
  }
}
```

Use the `token` as a Bearer token for subsequent API calls. Existing JWT-based endpoints remain unchanged.

## 4. Notes

- New OAuth users automatically receive the `NORMAL_USER` role.
- Users created via OAuth2 have a random internal password and cannot log in with email/password unless a password is later set.
- GitHub requires the user email to be public. If it is private, authentication fails with an explanatory message.

