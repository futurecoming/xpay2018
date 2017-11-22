CREATE TABLE IF NOT EXISTS bill_app (
	id BIGSERIAL PRIMARY KEY,
	app_key varchar(64) NOT NULL,
	app_secret varchar(256) NOT NULL,
	name varchar(64) NOT NULL,
	token varchar(32),
	create_date TIMESTAMP WITH TIME ZONE NOT NULL default now(), 
	update_date TIMESTAMP WITH TIME ZONE NOT NULL default now(),
	deleted boolean DEFAULT FALSE
);	
ALTER SEQUENCE bill_app_id_seq RESTART 10;
CREATE INDEX idx_app_key ON bill_app(app_key); 

CREATE TABLE IF NOT EXISTS bill_store (
	id BIGSERIAL PRIMARY KEY,
	code varchar(32) NOT NULL,
	name varchar(64) NOT NULL,
	bail NUMERIC NOT NULL default 0,
	non_bail NUMERIC NOT NULL default 0,
	bar NUMERIC NOT NULL default 1000,
	bail_percentage SMALLINT NOT NULL default 10,
	rotation_type varchar(16) NOT NULL default 'RoundRobin',
	rotation_index SMALLINT NOT NULL default 1,
	create_date TIMESTAMP WITH TIME ZONE NOT NULL default now(), 
	update_date TIMESTAMP WITH TIME ZONE NOT NULL default now(),
	deleted boolean DEFAULT FALSE
);
ALTER SEQUENCE bill_store_id_seq RESTART 100;
CREATE INDEX idx_bill_store_code ON bill_store(code); 

CREATE TABLE IF NOT EXISTS bill_store_channel (
	id BIGSERIAL PRIMARY KEY,
	store_id BIGINT NOT NULL,
	ext_store_id varchar(32) NOT NULL,
	priority INT NOT null default 1,
	payment_gateway varchar(16) NOT NULL,
	create_date TIMESTAMP WITH TIME ZONE NOT NULL default now(), 
	update_date TIMESTAMP WITH TIME ZONE NOT NULL default now(),
	deleted boolean DEFAULT FALSE
);
ALTER SEQUENCE bill_store_channel_id_seq RESTART 1000;
CREATE INDEX idx_bill_store_channel ON bill_store_channel(store_id); 


CREATE TABLE IF NOT EXISTS bill_store_link (
	id BIGSERIAL PRIMARY KEY,
	store_id BIGINT NOT NULL,
	app_link varchar(256) NOT NULL,
	create_date TIMESTAMP WITH TIME ZONE NOT NULL default now(), 
	update_date TIMESTAMP WITH TIME ZONE NOT NULL default now(),
	deleted boolean DEFAULT FALSE
);
ALTER SEQUENCE bill_store_link_id_seq RESTART 100;
CREATE INDEX idx_bill_store_link ON bill_store_link(store_id); 


CREATE TABLE IF NOT EXISTS bill_channel_limit (
	id BIGSERIAL PRIMARY KEY,
	channel_id BIGINT NOT NULL,
	channel_limit NUMERIC NOT NULL default 50000,
	current_amount NUMERIC NOT null default 0,
	notes TEXT,
	create_date TIMESTAMP WITH TIME ZONE NOT NULL default now(), 
	update_date TIMESTAMP WITH TIME ZONE NOT NULL default now(),
	deleted boolean DEFAULT FALSE
);
ALTER SEQUENCE bill_channel_limit_id_seq RESTART 1000;
CREATE INDEX idx_bill_channel_id ON bill_channel_limit(channel_id); 


CREATE TABLE IF NOT EXISTS bill_order (
	id BIGSERIAL PRIMARY KEY,
	order_no varchar(32) NOT NULL,
	app_id BIGINT NOT NULL,
	store_id BIGINT NOT NULL,
	store_channel BIGINT NOT NULL,
	total_fee varchar(10) NOT NULL,
	order_time varchar(14) NOT NULL,
	pay_channel varchar(16) NOT NULL,
	seller_order_no varchar(64),
	ext_order_no varchar(64),
	attach varchar(256),
	device_id varchar(32),
	ip varchar(32),
	notify_url varchar(256),
	code_url varchar(256),
	prepay_id varchar(64),
	token_id varchar(64),
	pay_info varchar(1024),
	status varchar(16),
	detail_id BIGINT,
	create_date TIMESTAMP WITH TIME ZONE NOT NULL default now(), 
	update_date TIMESTAMP WITH TIME ZONE NOT NULL default now(),
	deleted boolean DEFAULT FALSE
);
ALTER SEQUENCE bill_order_id_seq RESTART 1000;
CREATE INDEX idx_order_no ON bill_order(order_no); 
CREATE INDEX idx_ext_order_no ON bill_order(ext_order_no); 
CREATE INDEX idx_order_store_id ON bill_order(store_id, update_date); 
ALTER TABLE bill_order ADD COLUMN target_order_no varchar(64);
CREATE INDEX idx_target_order_no ON bill_order(target_order_no); 
CREATE INDEX idx_order_store_channel ON bill_order(store_channel); 


CREATE TABLE IF NOT EXISTS bill_order_detail (
	id BIGSERIAL PRIMARY KEY,
	store_name varchar(64),
	operator varchar(64),
	subject varchar(64),
	description varchar(256),
	items varchar(2048),
	create_date TIMESTAMP WITH TIME ZONE NOT NULL default now(), 
	update_date TIMESTAMP WITH TIME ZONE NOT NULL default now(),
	deleted boolean DEFAULT FALSE
);
ALTER SEQUENCE bill_order_detail_id_seq RESTART 1000;

CREATE TABLE IF NOT EXISTS bill_order_notification (
	id BIGSERIAL PRIMARY KEY,
	order_no varchar(32) NOT NULL,
	seller_order_no varchar(64),
	ext_order_no varchar(64),
	target_order_no varchar(64),
	status varchar(16),
	notify_url varchar(256),
	create_date TIMESTAMP WITH TIME ZONE NOT NULL default now(), 
);
ALTER SEQUENCE bill_order_notification_id_seq RESTART 1000;
CREATE INDEX idx_notification_order_no ON bill_order_notification(order_no); 

CREATE TABLE IF NOT EXISTS cash_transaction (
	id BIGSERIAL PRIMARY KEY,
	order_no varchar(32) NOT NULL,
	channel varchar(32) NOT NULL,
	card_num varchar(32) NOT NULL,
	account_name varchar(32) NOT NULL,
	bank_name varchar(32) NOT NULL,
	total_fee varchar(10) NOT NULL,
	status varchar(16) default 'PENDING',
	create_date TIMESTAMP WITH TIME ZONE NOT NULL default now(), 
	update_date TIMESTAMP WITH TIME ZONE NOT NULL default now(),
	deleted boolean DEFAULT FALSE
);
ALTER SEQUENCE cash_transaction_id_seq RESTART 1000;
CREATE INDEX idx_cash_transaction_order_no ON cash_transaction(order_no);
CREATE INDEX idx_cash_card_num ON cash_transaction(card_num); 

ALTER TABLE bill_app ADD COLUMN name VARCHAR(64);
ALTER TABLE bill_store_channel ADD COLUMN bill_type VARCHAR(32);
ALTER TABLE bill_store ADD COLUMN bail_store_id BIGINT;
ALTER TABLE bill_store ADD COLUMN app_id BIGINT;
ALTER TABLE bill_store ADD COLUMN daily_limit BIGINT default -1;
ALTER TABLE bill_order ADD COLUMN subject VARCHAR(256);
ALTER TABLE bill_store ADD COLUMN channels VARCHAR(256);
ALTER TABLE bill_store ADD COLUMN bail_channels VARCHAR(256);
ALTER TABLE bill_store ADD COLUMN csr_tel VARCHAR(256);
ALTER TABLE bill_store_channel ADD COLUMN ext_store_name VARCHAR(64);

ALTER TABLE bill_store DROP COLUMN bail_store_id;
ALTER TABLE bill_store_channel DROP COLUMN store_id;
ALTER TABLE bill_order ADD COLUMN return_url VARCHAR(256);
ALTER TABLE bill_store ADD COLUMN proxy_url VARCHAR(256);


CREATE TABLE IF NOT EXISTS bill_agent (
	id BIGSERIAL PRIMARY KEY,
	account varchar(32) NOT NULL,
	agent_password varchar(32) NOT NULL,
	name varchar(64) NOT NULL,
	csr_tel varchar(32),
	csr_wechat varchar(32),
	proxy_url VARCHAR(256),
	token varchar(32),
	create_date TIMESTAMP WITH TIME ZONE NOT NULL default now(), 
	update_date TIMESTAMP WITH TIME ZONE NOT NULL default now(),
	deleted boolean DEFAULT FALSE
);	
ALTER SEQUENCE bill_agent_id_seq RESTART 10;
CREATE INDEX idx_bill_agent_account ON bill_agent(account); 
CREATE INDEX idx_bill_agent_token ON bill_agent(token); 

ALTER TABLE bill_store ADD COLUMN agent_id BIGINT;
ALTER TABLE bill_store_channel ADD COLUMN agent_id BIGINT;
ALTER TABLE bill_app ADD COLUMN agent_id BIGINT;
CREATE INDEX idx_bill_app_agent ON bill_app(agent_id); 

ALTER table bill_app OWNER TO xpay;
ALTER table bill_store OWNER TO xpay;
ALTER table bill_store_channel OWNER TO xpay;
ALTER table bill_order OWNER TO xpay;
ALTER table bill_order_detail OWNER TO xpay;
ALTER table bill_store_link OWNER TO xpay;

