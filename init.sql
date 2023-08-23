CREATE TABLE executables (
  id character(36) NOT NULL,
  name character varying(40) NOT NULL,
  username character varying(40) NOT NULL,
  CONSTRAINT id_executable PRIMARY KEY (id)
);

CREATE TABLE users (
  username character varying(40) NOT NULL,
  password character varying(40) NOT NULL,
  CONSTRAINT id_user PRIMARY KEY (username)
);

ALTER TABLE executables add CONSTRAINT fk_executables
   FOREIGN KEY (username) REFERENCES users;
