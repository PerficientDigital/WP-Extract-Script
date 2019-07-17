# WordPress Extract Script

A script to process a [WordPress WRX Export File](https://en.support.wordpress.com/export/) into a format which can be imported using the [AEM Migration Script](https://github.com/PerficientDigital/AEM-Migration-Script). This script is primarily intended for migrating posts, including their assets and tags, but can be extended for other use cases.


```
bash-3.2$ ./wp-extract.groovy wordpress.xml ~/Desktop/export
Pre-processing WordPress Export wordpress.xml to /Users/dan.klco/Desktop/export
Batch ID (Wordpress): Batch1
Content Root (Required): /content/site 
Path Date Format (ex: yyyy/MM/): yyyy/MM/
Assets Root (Required): /content/dam/site
Domain Name (Required): https://www.site.com
Migration Template (Required): post
Download Attachments (Y/N): Y
Loading into
	Content Root: /content/site
	Assets Root: /content/dam/site
Using
	Domain: https://www.site.com
	Template: post
Continue (Y/N): Y
Setting up folders...
Parsing wordpress.xml...
Handling item of type attachment...
```

## Quickstart

To use the script:

1. Download a [WordPress WRX Export](https://en.support.wordpress.com/export/#export-your-content-to-another-blog-or-platform) file
2. Execute the Wordpress Export Script:
    `groovy wp-extract.groovy wordpress.xml ~/Desktop/export`
3. Configure the [Script Settings](#settings)
4. Enter "Y" to confirm the settings

The end result will be a directory with all of the posts under the subdirectory `work/source` and all of the CSV files in a subdirectory named the Batch ID. This can then be used by the [AEM Migration Script](https://github.com/PerficientDigital/AEM-Migration-Script) to import the content into AEM.

## Script Parameters

There are  two parameters to execute the script:

 - **wxr-file** -- This required parameter is the first parameter to the script. It should be [WordPress WRX Export File](https://en.support.wordpress.com/export/) containing the contents of the site to migrate.
 - **target** -- This required parameter is the second parameter to the script. This should be a directory to which the extracted content should be saved.
 
## Settings

The following files are used to control how the script is executed:

 - **Batch ID (Wordpress)** -- A batch ID string, saved into the content and used as the configuration folder name. The default will be "Wordpress"
 - **Content Root (Required)** -- The root path under which to save the Wordpress content in AEM
 - **Path Date Format (ex: yyyy/MM/)** -- A [Java Simple Date Format string](https://docs.oracle.com/javase/10/docs/api/java/text/SimpleDateFormat.html) to use to generate the date portion of the content path. Not required, but can be used to organize posts by date.
 - **Assets Root (Required)** -- The root path under which to save the Wordpress attachments. Each attachment will be stored in a folder with the year / month.
 - **Domain Name (Required)** -- The domain name of the site to be migrated including protocol but not including the initial slash. This is used to generate the URLs with which to download the attachments and the replacement.csv
 - **Migration Template (Required)** -- The [AEM Migration Script](https://github.com/PerficientDigital/AEM-Migration-Script) template name to use for the posts
 - **Download Attachments (Y/N)** -- Whether or not to download the attachments from Wordpress. If true, the attachments will be downloaded locally, if not, their metadata will be extracted, but they will not be downloaded.
 