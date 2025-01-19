package guy.shalev.ATnT.Home.assignment.constants;

public class UserQueries {
    public static final String CREATE_USER = """
        create table if not exists users(
            username varchar(50) not null primary key,
            password varchar(500) not null,
            enabled boolean not null
        )
    """;

    public static final String CREATE_AUTHORITY = """
        create table if not exists authorities (
            username varchar(50) not null,
            authority varchar(50) not null,
            constraint fk_authorities_users foreign key(username) references users(username)
        )
    """;
}
