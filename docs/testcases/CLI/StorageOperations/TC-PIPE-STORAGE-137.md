# Change storage policy with enabled fields

**Actions**:
1. Create storage with enabled sts, lts, b, versioning enabled
2. Change policy: `pipe storage policy -n storage_name -sts 20 -lts 30 -b 10 -v`
3. Change policy: `pipe storage policy -n storage_name -sts 20 -lts 30 -v`
4. Change policy: `pipe storage policy -n storage_name -v`
5. Delete storage

***
**Expected result:**

1. Check with Cloud Provider's assert policy utility: sts and lts enabled, backup duration enabled and ensure that values are correct
2. Check with Cloud Provider's assert policy utility: sts and lts enabled, backup duration enabled and ensure that values are correct
3. Check with Cloud Provider's assert policy utility: sts and lts enabled, backup duration enabled (=20 as default) and ensure that values are correct
4. Check with Cloud Provider's assert policy utility: sts and lts disabled, backup duration enabled (=20 as default) and ensure that values are correct
