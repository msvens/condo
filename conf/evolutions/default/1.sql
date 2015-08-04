# --- !Ups

CREATE TABLE token (
	id              VARCHAR(254) PRIMARY KEY,
	token           VARCHAR(254) NOT NULL,
	"refreshToken"  VARCHAR(254)
);

CREATE TABLE directory (
	id             VARCHAR(254) PRIMARY KEY,
	title          VARCHAR(254) NOT NULL,
	role           VARCHAR(254)
);
	
CREATE TABLE member (
	id            SERIAL PRIMARY KEY,
	name          VARCHAR(254) NOT NULL,
	email         VARCHAR(254) NOT NULL,
	phone         VARCHAR(254),
	apt           VARCHAR(254),
	role					VARCHAR(254)
);

CREATE TABLE config (
	id						SERIAL PRIMARY KEY,
	name					VARCHAR(254) NOT NULL,
	email					VARCHAR(254) NOT NULL,
	"rootDir"			VARCHAR(254) NOT NULL,
	description		VARCHAR,
	web						VARCHAR(254)
);

# --- !Downs
DROP TABLE token;
DROP TABLE directory;
DROP TABLE member;
DROP TABLE config;