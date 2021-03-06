# MV operation with folders: without --recursive

**Actions**:

1.	Create 2 storages (via pipe cli to register in the base) [`pipe storage create --path storage_name...`]
2.	Create folder (`resources/`)
3.	Check that following cases work with absolute paths (with `/` at the end of the path): 
    1.	Move test folder to created storage specifying the absolute path `[pipe storage mv ../resources/ cp://storage_name/test_folder/]`. 
    2.	Move the folder back to the local machine specifying the absolute path `[pipe storage mv cp://storage_name/test_folder/ .../resources/]`. 
    3.	Check that error is returned.
    4.	Move folder to the other storage using the absolute paths `[pipe storage mv cp://storage_name/test_folder/ cp://other_storage_name/test_folder_copy/]`
    5.	Check that error is returned.
4.	Delete storages.

***
**Expected result:**

*3, 5.* An error is returned if `--recursive` option is not specified