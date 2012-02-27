create or replace function ${rootName}.getTables_(schemaPattern text, tablePattern text, curs refcursor) returns refcursor as $$
begin
	open curs for SELECT quote_ident(n.nspname) || '.' || quote_ident(c.relname) as "QuotedName"
	FROM pg_catalog.pg_class c
	LEFT JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
	WHERE c.relkind IN ('r','') AND n.nspname NOT IN ('pg_catalog', 'pg_toast', 'information_schema') 
	AND n.nspname ~ ( '^('|| schemaPattern ||')$' ) AND c.relname ~ ( '^('|| tablePattern ||')$' ) ;
	return curs;
end;
$$ LANGUAGE plpgsql;

create or replace function ${rootName}.getTables(schemaPattern text, tablePattern text) returns refcursor as $$
begin
return ${rootName}.getTables_(schemaPattern, tablePattern, null);
end;
$$ LANGUAGE plpgsql;

create or replace function ${rootName}.getTables(schemaPattern text, tablePattern text, curs refcursor) returns void as $$
begin
	perform ${rootName}.getTables_(schemaPattern, tablePattern, curs);
end;
$$ LANGUAGE plpgsql;

create or replace function ${rootName}.cursorToSet(c refcursor) returns setof record as $$
declare
  r record;
begin
 	FETCH c INTO r;
 	while (found) loop
   		RAISE NOTICE 'adding %', c;
   		return next r;
   		FETCH c INTO r;
  	end loop;
end;
$$ LANGUAGE plpgsql;


create or replace function ${rootName}.setTrigger(able text, c refcursor) returns void as $$
DECLARE
n text;
begin
	FETCH c INTO n;
	while (found) loop
		RAISE NOTICE '% triggers on %', able, n;
		execute ('alter table ' || n || ' ' || able || ' trigger all' );
		FETCH c INTO n;
	end loop;
	return;
end;
$$ LANGUAGE plpgsql;

create or replace function ${rootName}."alterSeq"(seqName text, newValueReq text) returns text as $$
DECLARE
newValue text;
begin
	execute newValueReq into strict newValue;
	RAISE NOTICE 'restarting % to %', seqName, newValue;
	execute ('ALTER SEQUENCE ' || seqName || ' RESTART ' || newValue );
	return newValue;
end;
$$ LANGUAGE plpgsql;
