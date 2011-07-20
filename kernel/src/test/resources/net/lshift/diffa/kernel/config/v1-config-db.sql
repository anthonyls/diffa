create table category_descriptor (category_id integer generated by default as identity, constraint_type varchar(255) not null, prefix_length integer, max_length integer, step integer, primary key (category_id));
create table config_options (opt_key varchar(255) not null, opt_val varchar(255), is_internal smallint, primary key (opt_key));
create table endpoint (name varchar(255) not null, scan_url varchar(255), content_retrieval_url varchar(255), version_generation_url varchar(255), inbound_url varchar(255), content_type varchar(255) not null, inbound_content_type varchar(255), primary key (name));
create table endpoint_categories (id varchar(255) not null, category_descriptor_id integer not null, name varchar(255) not null, primary key (id, name));
create table escalations (name varchar(255) not null, pair_key varchar(255) not null, action varchar(255) not null, action_type varchar(255) not null, event varchar(255) not null, origin varchar(255) not null, primary key (name, pair_key));
create table pair (pair_key varchar(255) not null, upstream varchar(255) not null, downstream varchar(255) not null, version_policy_name varchar(255), matching_timeout integer, scan_cron_spec varchar(255), primary key (pair_key));
create table prefix_category_descriptor (id integer not null, primary key (id));
create table range_category_descriptor (id integer not null, data_type varchar(255), upper_bound varchar(255), lower_bound varchar(255), primary key (id));
create table repair_actions (name varchar(255) not null, pair_key varchar(255) not null, url varchar(255), scope varchar(255), primary key (name, pair_key));
create table set_category_descriptor (id integer not null, primary key (id));
create table set_constraint_values (value_id integer not null, value_name varchar(255) not null, primary key (value_id, value_name));
create table users (name varchar(255) not null, email varchar(255), primary key (name));
alter table endpoint_categories add constraint FKEE1F9F06BC780104 foreign key (id) references endpoint;
alter table endpoint_categories add constraint FKEE1F9F06B6D4F2CB foreign key (category_descriptor_id) references category_descriptor;
alter table pair add constraint FK3462DA25F0B1C4 foreign key (upstream) references endpoint;
alter table pair add constraint FK3462DA4242E68B foreign key (downstream) references endpoint;
alter table prefix_category_descriptor add constraint FK46474423466530AE foreign key (id) references category_descriptor;
alter table range_category_descriptor add constraint FKDC53C74E7A220B71 foreign key (id) references category_descriptor;
alter table set_category_descriptor add constraint FKA51D45F39810CA56 foreign key (id) references category_descriptor;
alter table set_constraint_values add constraint FK96C7B32744035BE4 foreign key (value_id) references category_descriptor;
insert into config_options (opt_key, opt_val, is_internal) values ('configStore.schemaVersion', '1', 1);