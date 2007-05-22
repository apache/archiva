-- mysql mysql --user=root --password < dev_bootstrap.sql

create database archiva;
create database redback;

use mysql;

GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP,ALTER,INDEX
    ON archiva.*
    TO 'archiva'@'localhost'
    IDENTIFIED BY 'sa';

GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP,ALTER,INDEX
    ON redback.*
    TO 'archiva'@'localhost'
    IDENTIFIED BY 'sa';

