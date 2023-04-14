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
import sk.db.relational.spring.services.RdbTransactionWrapper;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TransactionalNamedParameterJdbcTemplate extends NamedParameterJdbcTemplate {
    private final RdbTransactionWrapper transWrapper;

    public TransactionalNamedParameterJdbcTemplate(DataSource ds, RdbTransactionWrapper transWrapper) {
        super(ds);
        this.transWrapper = transWrapper;
    }

    @Override
    public JdbcOperations getJdbcOperations() {
        return transWrapper.transactional(() -> super.getJdbcOperations());
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return transWrapper.transactional(() -> super.getJdbcTemplate());
    }

    @Override
    public void setCacheLimit(int cacheLimit) {
        transWrapper.transactionalRun(() -> super.setCacheLimit(cacheLimit));
    }

    @Override
    public int getCacheLimit() {
        return transWrapper.transactional(() -> super.getCacheLimit());
    }

    @Override
    public <T> T execute(String sql, SqlParameterSource paramSource, PreparedStatementCallback<T> action)
            throws DataAccessException {
        return transWrapper.transactional(() -> super.execute(sql, paramSource, action));
    }

    @Override
    public <T> T execute(String sql, Map<String, ?> paramMap, PreparedStatementCallback<T> action)
            throws DataAccessException {
        return transWrapper.transactional(() -> super.execute(sql, paramMap, action));
    }

    @Override
    public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
        return transWrapper.transactional(() -> super.execute(sql, action));
    }

    @Override
    public <T> T query(String sql, SqlParameterSource paramSource, ResultSetExtractor<T> rse) throws DataAccessException {
        return transWrapper.transactional(() -> super.query(sql, paramSource, rse));
    }

    @Override
    public <T> T query(String sql, Map<String, ?> paramMap, ResultSetExtractor<T> rse) throws DataAccessException {
        return transWrapper.transactional(() -> super.query(sql, paramMap, rse));
    }

    @Override
    public <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException {
        return transWrapper.transactional(() -> super.query(sql, rse));
    }

    @Override
    public void query(String sql, SqlParameterSource paramSource, RowCallbackHandler rch) throws DataAccessException {
        transWrapper.transactionalRun(() -> super.query(sql, paramSource, rch));
    }

    @Override
    public void query(String sql, Map<String, ?> paramMap, RowCallbackHandler rch) throws DataAccessException {
        transWrapper.transactionalRun(() -> super.query(sql, paramMap, rch));
    }

    @Override
    public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
        transWrapper.transactionalRun(() -> super.query(sql, rch));
    }

    @Override
    public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
            throws DataAccessException {
        return transWrapper.transactional(() -> super.query(sql, paramSource, rowMapper));
    }

    @Override
    public <T> List<T> query(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper) throws DataAccessException {
        return transWrapper.transactional(() -> super.query(sql, paramMap, rowMapper));
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return transWrapper.transactional(() -> super.query(sql, rowMapper));
    }

    @Override
    public <T> T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
            throws DataAccessException {
        return transWrapper.transactional(() -> super.queryForObject(sql, paramSource, rowMapper));
    }

    @Override
    public <T> T queryForObject(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper) throws DataAccessException {
        return transWrapper.transactional(() -> super.queryForObject(sql, paramMap, rowMapper));
    }

    @Override
    public <T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType)
            throws DataAccessException {
        return transWrapper.transactional(() -> super.queryForObject(sql, paramSource, requiredType));
    }

    @Override
    public <T> T queryForObject(String sql, Map<String, ?> paramMap, Class<T> requiredType) throws DataAccessException {
        return transWrapper.transactional(() -> super.queryForObject(sql, paramMap, requiredType));
    }

    @Override
    public Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) throws DataAccessException {
        return transWrapper.transactional(() -> super.queryForMap(sql, paramSource));
    }

    @Override
    public Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap) throws DataAccessException {
        return transWrapper.transactional(() -> super.queryForMap(sql, paramMap));
    }

    @Override
    public <T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType)
            throws DataAccessException {
        return transWrapper.transactional(() -> super.queryForList(sql, paramSource, elementType));
    }

    @Override
    public <T> List<T> queryForList(String sql, Map<String, ?> paramMap, Class<T> elementType)
            throws DataAccessException {
        return transWrapper.transactional(() -> super.queryForList(sql, paramMap, elementType));
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource) throws DataAccessException {
        return transWrapper.transactional(() -> super.queryForList(sql, paramSource));
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap) throws DataAccessException {
        return transWrapper.transactional(() -> super.queryForList(sql, paramMap));
    }

    @Override
    public SqlRowSet queryForRowSet(String sql, SqlParameterSource paramSource) throws DataAccessException {
        return transWrapper.transactional(() -> super.queryForRowSet(sql, paramSource));
    }

    @Override
    public SqlRowSet queryForRowSet(String sql, Map<String, ?> paramMap) throws DataAccessException {
        return transWrapper.transactional(() -> super.queryForRowSet(sql, paramMap));
    }

    @Override
    public int update(String sql, SqlParameterSource paramSource) throws DataAccessException {
        return transWrapper.transactional(() -> super.update(sql, paramSource));
    }

    @Override
    public int update(String sql, Map<String, ?> paramMap) throws DataAccessException {
        return transWrapper.transactional(() -> super.update(sql, paramMap));
    }

    @Override
    public int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder)
            throws DataAccessException {
        return transWrapper.transactional(() -> super.update(sql, paramSource, generatedKeyHolder));
    }

    @Override
    public int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder, String[] keyColumnNames)
            throws DataAccessException {
        return transWrapper.transactional(() -> super.update(sql, paramSource, generatedKeyHolder, keyColumnNames));
    }

    @Override
    public int[] batchUpdate(String sql, Map<String, ?>[] batchValues) {
        return transWrapper.transactional(() -> super.batchUpdate(sql, batchValues));
    }

    @Override
    public int[] batchUpdate(String sql, SqlParameterSource[] batchArgs) {
        return transWrapper.transactional(() -> super.batchUpdate(sql, batchArgs));
    }

    @Override
    protected PreparedStatementCreator getPreparedStatementCreator(String sql, SqlParameterSource paramSource) {
        return transWrapper.transactional(() -> super.getPreparedStatementCreator(sql, paramSource));
    }

    @Override
    protected PreparedStatementCreator getPreparedStatementCreator(String sql, SqlParameterSource paramSource,
            Consumer<PreparedStatementCreatorFactory> customizer) {
        return transWrapper.transactional(() -> super.getPreparedStatementCreator(sql, paramSource, customizer));
    }

    @Override
    protected ParsedSql getParsedSql(String sql) {
        return transWrapper.transactional(() -> super.getParsedSql(sql));
    }

    @Override
    protected PreparedStatementCreatorFactory getPreparedStatementCreatorFactory(ParsedSql parsedSql,
            SqlParameterSource paramSource) {
        return transWrapper.transactional(() -> super.getPreparedStatementCreatorFactory(parsedSql, paramSource));
    }
}
