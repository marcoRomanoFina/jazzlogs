alter table album_logs
    alter column best_moment drop default;

alter table album_logs
    alter column best_moment type jsonb
    using case
        when best_moment is null or btrim(best_moment) = '' then null
        else jsonb_build_object(
            'introduccion', best_moment,
            'momentos', '[]'::jsonb,
            'conclusion', ''
        )
    end;
