package sk.db.relational.spring;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TransactionalNamedParameterJdbcTemplate extends NamedParameterJdbcTemplate {
    public TransactionalNamedParameterJdbcTemplate(DataSource ds) {super(ds);}

    @Override
    @Transactional
    public JdbcOperations getJdbcOperations() {
        return super.getJdbcOperations();
    }

    @Override
    @Transactional
    public JdbcTemplate getJdbcTemplate() {
        return super.getJdbcTemplate();
    }

    @Override
    @Transactional
    public void setCacheLimit(int cacheLimit) {
        super.setCacheLimit(cacheLimit);
    }

    @Override
    @Transactional
    public int getCacheLimit() {
        return super.getCacheLimit();
    }

    @Override
    @Transactional
    public <T> T execute(String sql, SqlParameterSource paramSource, PreparedStatementCallback<T> action)
            throws DataAccessException {
        return super.execute(sql, paramSource, action);
    }

    @Override
    @Transactional
    public <T> T execute(String sql, Map<String, ?> paramMap, PreparedStatementCallback<T> action)
            throws DataAccessException {
        return super.execute(sql, paramMap, action);
    }

    @Override
    @Transactional
    public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
        return super.execute(sql, action);
    }

    @Override
    @Transactional
    public <T> T query(String sql, SqlParameterSource paramSource, ResultSetExtractor<T> rse) throws DataAccessException {
        return super.query(sql, paramSource, rse);
    }

    @Override
    @Transactional
    public <T> T query(String sql, Map<String, ?> paramMap, ResultSetExtractor<T> rse) throws DataAccessException {
        return super.query(sql, paramMap, rse);
    }

    @Override
    @Transactional
    public <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException {
        return super.query(sql, rse);
    }

    @Override
    @Transactional
    public void query(String sql, SqlParameterSource paramSource, RowCallbackHandler rch) throws DataAccessException {
        super.query(sql, paramSource, rch);
    }

    @Override
    @Transactional
    public void query(String sql, Map<String, ?> paramMap, RowCallbackHandler rch) throws DataAccessException {
        super.query(sql, paramMap, rch);
    }

    @Override
    @Transactional
    public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
        super.query(sql, rch);
    }

    @Override
    @Transactional
    public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
            throws DataAccessException {
        return super.query(sql, paramSource, rowMapper);
    }

    @Override
    @Transactional
    public <T> List<T> query(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper) throws DataAccessException {
        return super.query(sql, paramMap, rowMapper);
    }

    @Override
    @Transactional
    public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return super.query(sql, rowMapper);
    }

    @Override
    @Transactional
    public <T> T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
            throws DataAccessException {
        return super.queryForObject(sql, paramSource, rowMapper);
    }

    @Override
    @Transactional
    public <T> T queryForObject(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper) throws DataAccessException {
        return super.queryForObject(sql, paramMap, rowMapper);
    }

    @Override
    @Transactional
    public <T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType)
            throws DataAccessException {
        return super.queryForObject(sql, paramSource, requiredType);
    }

    @Override
    @Transactional
    public <T> T queryForObject(String sql, Map<String, ?> paramMap, Class<T> requiredType) throws DataAccessException {
        return super.queryForObject(sql, paramMap, requiredType);
    }

    @Override
    @Transactional
    public Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) throws DataAccessException {
        return super.queryForMap(sql, paramSource);
    }

    @Override
    @Transactional
    public Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap) throws DataAccessException {
        return super.queryForMap(sql, paramMap);
    }

    @Override
    @Transactional
    public <T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType)
            throws DataAccessException {
        return super.queryForList(sql, paramSource, elementType);
    }

    @Override
    @Transactional
    public <T> List<T> queryForList(String sql, Map<String, ?> paramMap, Class<T> elementType)
            throws DataAccessException {
        return super.queryForList(sql, paramMap, elementType);
    }

    @Override
    @Transactional
    public List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource) throws DataAccessException {
        return super.queryForList(sql, paramSource);
    }

    @Override
    @Transactional
    public List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap) throws DataAccessException {
        return super.queryForList(sql, paramMap);
    }

    @Override
    @Transactional
    public SqlRowSet queryForRowSet(String sql, SqlParameterSource paramSource) throws DataAccessException {
        return super.queryForRowSet(sql, paramSource);
    }

    @Override
    @Transactional
    public SqlRowSet queryForRowSet(String sql, Map<String, ?> paramMap) throws DataAccessException {
        return super.queryForRowSet(sql, paramMap);
    }

    @Override
    @Transactional
    public int update(String sql, SqlParameterSource paramSource) throws DataAccessException {
        return super.update(sql, paramSource);
    }

    @Override
    @Transactional
    public int update(String sql, Map<String, ?> paramMap) throws DataAccessException {
        return super.update(sql, paramMap);
    }

    @Override
    @Transactional
    public int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder)
            throws DataAccessException {
        return super.update(sql, paramSource, generatedKeyHolder);
    }

    @Override
    @Transactional
    public int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder, String[] keyColumnNames)
            throws DataAccessException {
        return super.update(sql, paramSource, generatedKeyHolder, keyColumnNames);
    }

    @Override
    @Transactional
    public int[] batchUpdate(String sql, Map<String, ?>[] batchValues) {
        return super.batchUpdate(sql, batchValues);
    }

    @Override
    @Transactional
    public int[] batchUpdate(String sql, SqlParameterSource[] batchArgs) {
        return super.batchUpdate(sql, batchArgs);
    }

    @Override
    @Transactional
    protected PreparedStatementCreator getPreparedStatementCreator(String sql, SqlParameterSource paramSource) {
        return super.getPreparedStatementCreator(sql, paramSource);
    }

    @Override
    @Transactional
    protected PreparedStatementCreator getPreparedStatementCreator(String sql, SqlParameterSource paramSource,
            Consumer<PreparedStatementCreatorFactory> customizer) {
        return super.getPreparedStatementCreator(sql, paramSource, customizer);
    }

    @Override
    @Transactional
    protected ParsedSql getParsedSql(String sql) {
        return super.getParsedSql(sql);
    }

    @Override
    @Transactional
    protected PreparedStatementCreatorFactory getPreparedStatementCreatorFactory(ParsedSql parsedSql,
            SqlParameterSource paramSource) {
        return super.getPreparedStatementCreatorFactory(parsedSql, paramSource);
    }
}
