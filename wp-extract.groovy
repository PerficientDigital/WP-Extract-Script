#!/usr/bin/env groovy

import groovy.io.FileType
import groovy.time.TimeCategory 
import groovy.transform.Field
import groovy.util.XmlSlurper
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

import java.text.SimpleDateFormat

@Grab('com.opencsv:opencsv:4.5')
import com.opencsv.CSVWriter;

// configure settings here
@Field 
final SEPARATOR = ","
@Field 
final ENCODING = "UTF-8"

// Get the settings for this batch
@Field
def batch = ''
@Field
def contentRoot = ''
@Field
def dateFormat = ''
@Field
def assetsRoot = ''
@Field
def domain = ''
@Field
def template = ''
@Field
def download = true
@Field
def assetsCsv = null
@Field
def fileMappings = null
@Field
def replacements = null
@Field
def pageCsv = null
@Field
def targetDir = null

def start = new Date()
if(args.length < 2) {
    println '\nUsage: groovy wp-preprocessor.groovy [wxr-file] [target]\n'
    System.exit(1)
}

println "Pre-processing WordPress Export ${args[0]} to ${args[1]}"
targetDir = new File(args[1]);
batch = System.console().readLine("Batch ID (Wordpress): ") ?: "Wordpress"
contentRoot = System.console().readLine("Content Root (Required): ")
assert contentRoot
dateFormat = System.console().readLine("Path Date Format (ex: yyyy/MM/): ")
assetsRoot = System.console().readLine("Assets Root (Required): ")
assert assetsRoot
domain = System.console().readLine("Domain Name (Required): ")
assert domain
template = System.console().readLine("Migration Template (Required): ")
assert template
download = System.console().readLine("Download Attachments (Y/N): ").toUpperCase() == 'Y'

println "Loading into\n\tContent Root: ${contentRoot}\n\tAssets Root: ${assetsRoot}\nUsing\n\tDomain: ${domain}\n\tTemplate: ${template}"
assert System.console().readLine("Continue (Y/N): ").toUpperCase() == 'Y'

println 'Setting up folders...'
if(!targetDir.exists()){
    targetDir.mkdirs()
}
new File(batch, targetDir).mkdirs();

println 'Writing data files...'
assetsCsv = new CSVWriter(new OutputStreamWriter(new FileOutputStream(new File("${batch}/asset-metadata.csv", targetDir)),ENCODING))
assetsCsv.writeNext(['assetPath','dc:title{{String}}','dc:description{{String}}'] as String[])
fileMappings = new CSVWriter(new OutputStreamWriter(new FileOutputStream(new File("${batch}/file-mappings.csv", targetDir)),ENCODING))
fileMappings.writeNext(['Status','Source','Target'] as String[])
replacements = new CSVWriter(new OutputStreamWriter(new FileOutputStream(new File("${batch}/replacements.csv", targetDir)),ENCODING))
replacements.writeNext(['Status','Source','Target'] as String[])
pageCsv = new CSVWriter(new OutputStreamWriter(new FileOutputStream(new File("${batch}/page-mappings.csv", targetDir)),ENCODING))
pageCsv.writeNext(['Status','Source Path','New Url','New Path','Template','Legacy Url','Redirects','Subnav Root?','Page Title','Page Description','Batch'] as String[])

println "Parsing ${args[0]}..."
def inXml = new XmlSlurper(false,true).parseText(new File(args[0]).getText(ENCODING))

void downloadFile(String url, String localPath) {
    def file = new File("work/source/${localPath}", targetDir)
    
    println "Downloading ${url} to ${file}"
    
    println "Creating parent folder..."
    file.getParentFile().mkdirs()
    println "Writing to file: ${file}"
    
    file.withOutputStream { stream ->
        file << fetch(url)
    }
}

InputStream fetch(String url){
    def get = new URL(url).openConnection()
    get.setRequestProperty('User-Agent', 'curl/7.35.0')
    def rc = get.getResponseCode()
    if(rc == 200){
        return get.getInputStream()
    }
    println "Retrieved invalid response code ${rc} from ${url}"
    return null
}

void handleAttachment(Object item){
    println "Handling attachment ${item.post_name}"
    
    def oldPath = item.attachment_url.text().replace(domain,'')
    def newPath = "${assetsRoot}${oldPath.replace('wp-content/uploads/','')}"
    if(download){
        downloadFile(item.attachment_url.text(), oldPath)
    }
    
    println "Adding entry ${[newPath,item.title.text().replaceAll("[\n\r]", "").trim(),item.encoded.text().trim()]} to asset-metadata.csv for ${oldPath}"
    assetsCsv.writeNext([newPath,item.title.text().replaceAll("[\n\r]", "").trim(),item.encoded.text().trim()] as String[])
    assetsCsv.flush()
    
    println "Adding entry to file-mappings.csv for ${oldPath}"
    fileMappings.writeNext(['Migrate',oldPath,newPath] as String[])
    fileMappings.flush()
    
    println "Adding entry to replacements.csv for ${item.post_id}"
    replacements.writeNext(['Migrate','wordpress-image-'+item.post_id,newPath] as String[])
    replacements.flush()
    
}

void handlePost(Object item) {
    println "Handling post ${item.post_name}"
    
    if(item.status.text() == 'draft'){
        println "Skipping ${item.post_name} as it is in draft..."
        return
    }
    
    def itemFile = new File("work/source/${item.post_name}.xml", targetDir)
    itemFile.getParentFile().mkdirs()
    println "Saving item to: ${itemFile}..."
    
    XmlUtil xmlUtil = new XmlUtil()  
    xmlUtil.serialize(item, new FileWriter(itemFile))
    
    println "Adding line to page mappings for: ${item.post_name}.xml"
    def date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(item.post_date.text())
    def path ="${contentRoot}/${new SimpleDateFormat(dateFormat).format(date)}${item.post_name}"
    println "Mapping to new path: ${path}..."
    pageCsv.writeNext(['Migrate',"${item.post_name}.xml","${path}.html",path,template,item.link.text().replace(domain,''),'','No',item.title.text().replaceAll("[\n\r]", "").trim(),item.encoded[1].text().replaceAll("[\n\r]", "").trim(),batch] as String[])
    pageCsv.flush()
}

def attachments = 0
def posts = 0
def other = 0
inXml.channel.item.each{ item -> 
    println "Handling item of type ${item['post_type'].text()}..."
    
    if('attachment'.equals(item.post_type.text())) {
        handleAttachment(item)
        attachments++
    } else if ('post'.equals(item.post_type.text())) {
        handlePost(item)
        posts++
    } else {
        println "Unable to handle ${item.post_type}!!"
        other++
    }
}

if (other > 0) {
    println "${other} unknown files ignored!"
}
println "${posts} posts and ${attachments} attachments downloaded in ${TimeCategory.minus(new Date(), start)}"