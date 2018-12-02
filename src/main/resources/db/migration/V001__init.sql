create table hibernate_sequence (next_val bigint) engine=InnoDB;
insert into hibernate_sequence(next_val) values(1);

create table film (
    id bigint not null,
    link varchar(255),
    plot varchar(255),
    poster varchar(255),
    score double precision not null,
    title varchar(255),
    year integer not null,
    primary key (id)
) engine=InnoDB CHARACTER SET 'utf8';

create table film_genres (
    film_id bigint not null,
    genres_id bigint not null,
    primary key (film_id, genres_id)
) engine=InnoDB CHARACTER SET 'utf8';

create table genre (
    id bigint not null,
    name varchar(255),
    primary key (id)
) engine=InnoDB CHARACTER SET 'utf8';

create table score (
    id bigint not null,
    grade double precision not null,
    quantity bigint not null,
    film_id bigint,
    primary key (id)
) engine=InnoDB CHARACTER SET 'utf8';

alter table film_genres add constraint fk_film_genres_genres_id foreign key (genres_id) references genre (id);
alter table film_genres add constraint fk_film_genres_film_id foreign key (film_id) references film (id);
alter table score add constraint fk_score_film_id foreign key (film_id) references film (id);
