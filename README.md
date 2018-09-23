# PPMDocDownloader
Download documents in requests from HP PPM

Install Oracle driver to local maven repository:
install:install-file -Dfile=..pathto/ojdbc8.jar -DgroupId=com.oracle -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar

Create config.properties in project folder with following properties:
    
    ppm.url = 
    ppm.user = 
    ppm.password = 

    db.serverName = 
    db.port = 
    db.driverType = thin
    db.name = 
    db.user = 
    db.password = 
    db.query = select r.request_id, 'http:///itg/servlet/Document?ID='||d.document_id||'&viewOnly=TRUE' as URL, d.DOCUMENT_ID, d.NAME, d.EXTENSION  \
      from kcrt_requests r,knta_documents d where r.request_id = d.parent_primary_key and r.status_code = 'IN_PROGRESS'
