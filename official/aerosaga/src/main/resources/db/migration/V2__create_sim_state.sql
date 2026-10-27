-- Single-row table holding the simulator state (drones, airspace slots, temporary zones),
-- so in-flight missions survive an app restart.
create table sim_state (
    id          integer primary key,
    state_text  varchar(100000) not null,
    updated_at  timestamp with time zone not null
);

insert into sim_state (id, state_text, updated_at) values (1, '', current_timestamp);
