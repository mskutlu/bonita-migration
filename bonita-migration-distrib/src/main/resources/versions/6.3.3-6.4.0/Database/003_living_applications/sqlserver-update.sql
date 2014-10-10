CREATE TABLE business_app (
  tenantId NUMERIC(19, 0) NOT NULL,
  id NUMERIC(19, 0) NOT NULL,
  token NVARCHAR(50) NOT NULL,
  version NVARCHAR(50) NOT NULL,
  profileId NUMERIC(19, 0),
  description NVARCHAR(MAX),
  iconPath NVARCHAR(255),
  creationDate NUMERIC(19, 0) NOT NULL,
  createdBy NUMERIC(19, 0) NOT NULL,
  lastUpdateDate NUMERIC(19, 0) NOT NULL,
  updatedBy NUMERIC(19, 0) NOT NULL,
  state NVARCHAR(30) NOT NULL,
  homePageId NUMERIC(19, 0),
  displayName NVARCHAR(255) NOT NULL
)
@@

ALTER TABLE business_app ADD CONSTRAINT pk_business_app PRIMARY KEY (tenantid, id)
@@
ALTER TABLE business_app ADD CONSTRAINT uk_app_token_version UNIQUE (tenantId, token, version)
@@
ALTER TABLE business_app ADD CONSTRAINT fk_app_tenantId FOREIGN KEY (tenantid) REFERENCES tenant(id)
@@
ALTER TABLE business_app ADD CONSTRAINT fk_app_profileId FOREIGN KEY (tenantid, profileId) REFERENCES profile (tenantid, id)
@@

CREATE INDEX idx_app_token ON business_app (token, tenantid)
@@

CREATE TABLE business_app_page (
  tenantId NUMERIC(19, 0) NOT NULL,
  id NUMERIC(19, 0) NOT NULL,
  applicationId NUMERIC(19, 0) NOT NULL,
  pageId NUMERIC(19, 0) NOT NULL,
  token NVARCHAR(255) NOT NULL
)
@@

ALTER TABLE business_app_page ADD CONSTRAINT pk_business_app_page PRIMARY KEY (tenantid, id)
@@
ALTER TABLE business_app_page ADD CONSTRAINT uk_app_page_appId_token UNIQUE (tenantId, applicationId, token)
@@
ALTER TABLE business_app_page ADD CONSTRAINT fk_app_page_tenantId FOREIGN KEY (tenantid) REFERENCES tenant(id)
@@
ALTER TABLE business_app_page ADD CONSTRAINT fk_bus_app_id FOREIGN KEY (tenantid, applicationId) REFERENCES business_app (tenantid, id) ON DELETE CASCADE
@@
ALTER TABLE business_app_page ADD CONSTRAINT fk_page_id FOREIGN KEY (tenantid, pageId) REFERENCES page (tenantid, id)
@@

CREATE INDEX idx_app_page_token ON business_app_page (applicationId, token, tenantid)
@@
CREATE INDEX idx_app_page_pageId ON business_app_page (pageId, tenantid)
@@

INSERT INTO sequence (tenantid, id, nextid)
	SELECT id, 10200, 1 FROM tenant
	ORDER BY id ASC
@@

INSERT INTO sequence (tenantid, id, nextid)
	SELECT id, 10201, 1 FROM tenant
	ORDER BY id ASC
@@
