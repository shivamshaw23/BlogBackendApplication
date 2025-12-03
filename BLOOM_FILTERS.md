# Bloom Filter Usage

This project uses Guava bloom filters for two high-churn lookups where we want to reject duplicates without always querying the database:

- **User email registrations** – prevents most duplicate signups before a database hit.
- **Post titles** – rejects already-used titles during `createPost`.

## Configuration

The filters are controlled via the following properties (already defined in `application.properties` and override-able per profile/env):

```
bloom.email.expectedInsertions=10000
bloom.email.fpp=0.0001
bloom.post.expectedInsertions=20000
bloom.post.fpp=0.0001
```

Tune `expectedInsertions` based on your dataset size and adjust the false-positive probability (`fpp`) as desired. Larger insertions or lower `fpp` increase memory usage.

## Runtime Behavior

`BloomFilterService` loads all current emails and post titles on startup, then exposes helpers to:

- `isEmailProbablyRegistered` / `recordEmail`
- `isPostTitleProbablyUsed` / `recordPostTitle`

The filters are append-only (standard bloom filter limitation). If you bulk-delete data or expect many deletions, restart the service to rebuild the filters or add a scheduled rebuild job.

## API Impact

- `UserServiceImpl.registerNewUser` and `createUser` throw an `ApiException` when the bloom filter signals that an email is already registered.
- `PostServiceImpl.createPost` throws an `ApiException` for duplicated titles.

A tiny false-positive rate means one of these exceptions might occasionally trigger for a truly new value. The trade-off is faster duplicate rejection with minimal database load. Adjust the configuration if you need fewer false positives.
# Bloom Filters

This service now uses Guava bloom filters to short-circuit obvious duplicates before touching the database.

## What’s covered
- **User registration**: incoming email addresses are checked against an in-memory bloom filter. If the filter says the email already exists, the request is rejected immediately with an `ApiException`. Successful registrations add the email back into the filter.
- **Post creation**: new post titles go through the same treatment so duplicate titles can be rejected early. Persisted titles are recorded in the filter right after the post is saved.

## Warm-up strategy
`BloomFilterService` loads all existing user emails and post titles at startup (`@PostConstruct`) so it always begins with an accurate snapshot. Expected insertion counts and false-positive probabilities are configurable via `application.properties`:

```
bloom.email.expectedInsertions=20000
bloom.email.fpp=0.0001
bloom.post.expectedInsertions=50000
bloom.post.fpp=0.0001
```

Tune the numbers to match your dataset size and acceptable false-positive rate. Larger `expectedInsertions` or smaller `fpp` values consume more memory.

## Limitations
- Bloom filters can return false positives. When that happens, the API rejects the request even though the record may not exist. Clients should surface the error to users and let them try an alternative email/title.
- Filters live in-memory; restart the application to rebuild them from the database if they drift out of sync.

