package com.github.alittlehuang.data.jdbc.sql.mysql57;

import com.github.alittlehuang.data.metamodel.Attribute;
import com.github.alittlehuang.data.metamodel.EntityInformation;
import com.github.alittlehuang.data.jdbc.sql.PrecompiledSql;
import com.github.alittlehuang.data.jdbc.sql.PrecompiledSqlForEntity;
import com.github.alittlehuang.data.jdbc.sql.SelectedAttribute;
import com.github.alittlehuang.data.jdbc.sql.SqlBuilder;
import com.github.alittlehuang.data.query.specification.*;
import com.github.alittlehuang.data.util.Assert;
import com.github.alittlehuang.data.util.JointKey;

import javax.persistence.LockModeType;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.util.*;

public class Mysql57SqlBuilder implements SqlBuilder {

    @Override
    public PrecompiledSql count(Criteria<?> criteria) {
        return new Builder<>(criteria).buildCount();
    }

    @Override
    public PrecompiledSql exists(Criteria<?> criteria) {
        return new Builder<>(criteria).buildExists();
    }

    @Override
    public <T> PrecompiledSqlForEntity<T> listResult(Criteria<T> criteria) {
        return new Builder<>(criteria).buildListResult();
    }

    @Override
    public PrecompiledSql ListObjects(Criteria<?> criteria) {
        List<? extends Selection<?>> selections = criteria.getSelections();
        if ( selections == null || selections.isEmpty() ) {
            throw new RuntimeException("the selections must not be empty");
        }
        return new Builder<>(criteria).buildListObjects();
    }

    private static class Builder<T> {
        Criteria<T> criteria;
        EntityInformation<T, ?> rootEntityInfo;
        List<Object> args = new ArrayList<>();
        List<SelectedAttribute> selectedAttributes;
        Map<JointKey, JoinAttr> joinAttrs;

        StringBuilder sql;

        Builder(Criteria<T> criteria) {
            rootEntityInfo = EntityInformation.getInstance(criteria.getJavaType());
            this.criteria = criteria;
        }

        PrecompiledSqlForEntity<T> buildListResult() {
            sql = new StringBuilder();
            appendSelectFromEntity();
            int index = sql.length();
            appendWhereClause();
            appendOrders();
            insertJoin(index);
            appendLimit();
            appendLockMode();
            return new PrecompiledSqlForEntity<>(sql.toString(), args, selectedAttributes);
        }

        public PrecompiledSql buildListObjects() {
            sql = new StringBuilder();
            appendSelections();
            int index = sql.length();
            appendWhereClause();
            appendOrders();
            insertJoin(index);
            appendLimit();
            appendLockMode();
            return new PrecompiledSql(sql.toString(), args);
        }

        PrecompiledSql buildCount() {
            sql = new StringBuilder();
            sql.append("SELECT COUNT(1) AS `count_").append(rootEntityInfo.getTableName()).append("` ");
            appendFrom(rootEntityInfo);
            int index = sql.length();
            appendWhereClause();
            insertJoin(index);
            return new PrecompiledSql(sql.toString(), args);
        }

        public PrecompiledSql buildExists() {
            sql = new StringBuilder();
            sql.append("SELECT ");
            appendRootTableAlias();
            sql.append(".`").append(rootEntityInfo.getIdAttribute().getColumnName())
                    .append("` ");
            appendFrom(rootEntityInfo);
            int index = sql.length();
            appendWhereClause();
            insertJoin(index);
            sql.append(" LIMIT 1");
            return new PrecompiledSql(sql.toString(), args);
        }

        private void appendLimit() {
            Long offset = criteria.getOffset();
            Long maxResults = criteria.getMaxResults();
            if ( offset != null || maxResults != null ) {
                sql.append(" LIMIT ")
                        .append(offset == null ? 0 : offset)
                        .append(',')
                        .append(maxResults == null ? Long.MAX_VALUE : maxResults);
            }
        }

        private void appendLockMode() {
            LockModeType lockModeType = criteria.getLockModeType();
            if ( lockModeType != null ) {
                switch ( lockModeType ) {
                    case PESSIMISTIC_READ:
                        sql.append(" LOCK IN SHARE MODE");
                        break;
                    case PESSIMISTIC_WRITE:
                    case PESSIMISTIC_FORCE_INCREMENT:
                        sql.append(" FOR UPDATE");
                        break;
                    default:
                        break;
                }
            }
        }

        private void appendOrders() {
            List<? extends Orders<T>> orders = criteria.getOrders();
            if ( orders != null && !orders.isEmpty() ) {
                sql.append(" ORDER BY ");
                boolean first = true;
                for ( Orders<T> order : orders ) {
                    if ( first ) {
                        first = false;
                    } else {
                        sql.append(',');
                    }
                    appendAttribute(order);
                    sql.append(" ").append(order.getDirection());
                }

            }
        }

        private void appendWhereClause() {
            WhereClause<T> whereClause = criteria.getWhereClause();
            if ( whereClause != null && ( !whereClause.isCompound() || !whereClause.getCompoundItems().isEmpty() ) ) {
                sql.append(" WHERE ");
                appendWhereClause(whereClause);
            }
        }

        private void insertJoin(int index) {
            if ( joinAttrs != null && !joinAttrs.isEmpty() ) {
                StringBuilder join = new StringBuilder();
                for ( JoinAttr value : joinAttrs.values() ) {
                    buildJoin(join, value);
                }
                sql.insert(index, join);
            }
        }

        private void buildJoin(StringBuilder sql, JoinAttr joinAttr) {
            JoinAttr parent = joinAttr.parent;
            if ( parent != null ) {
                buildJoin(sql, parent);
            }
            if ( !joinAttr.appended ) {
                joinAttr.appended = true;
                sql.append(" ").append(joinAttr.joinType).append(" JOIN `")
                        .append(joinAttr.attrInfo.getTableName())
                        .append("` ");
                joinAttr.appendAlias(sql);
                sql.append(" ON ");

                if ( parent != null ) {
                    parent.appendAlias(sql);
                } else {
                    appendRootTableAlias(sql);
                }

                sql.append(".`").append(joinAttr.attribute.getJoinColumn().name()).append("`=");
                joinAttr.appendAlias(sql);
                String referenced = joinAttr.attribute.getJoinColumn().referencedColumnName();
                if ( referenced.length() == 0 ) {
                    referenced = joinAttr.attrInfo.getIdAttribute().getColumnName();
                }
                sql.append(".`").append(referenced).append('`');

            }
        }

        private void appendFrom(EntityInformation entityInfo) {
            sql.append("FROM `")
                    .append(entityInfo.getTableName())
                    .append("` ");
            appendRootTableAlias();
        }

        private void appendSelectFromEntity() {
            selectedAttributes = new ArrayList<>();
            sql.append("SELECT");
            boolean first = true;
            for ( Attribute<T, Object> attribute : rootEntityInfo.getBasicAttributes() ) {
                if ( first ) {
                    sql.append(" ");
                    first = false;
                } else {
                    sql.append(",");
                }
                appendRootTableAlias();
                sql.append('.');
                appendColumnName(attribute);
                selectedAttributes.add(new SelectedAttribute(attribute));
            }
            List<? extends FetchAttribute<T>> fetchList = criteria.getFetchAttributes();
            if ( fetchList != null && !fetchList.isEmpty() ) {
                for ( FetchAttribute<T> fetch : fetchList ) {
                    String[] names = fetch.getNames();
                    String[] tmp = new String[names.length + 1];
                    System.arraycopy(names, 0, tmp, 0, names.length);

                    Attribute<?, ?> attr = rootEntityInfo.getAttribute(names[0]);
                    EntityInformation<?, ?> attrInfo = EntityInformation.getInstance(attr.getFieldType());
                    SelectedAttribute p = new SelectedAttribute(attr, null);
                    if ( names.length > 1 ) {
                        for ( int i = 1; i < names.length; i++ ) {
                            attr = attrInfo.getAttribute(names[i]);
                            attrInfo = EntityInformation.getInstance(attr.getFieldType());
                            p = new SelectedAttribute(attr, p);
                        }
                    }
                    for ( Attribute<?, ?> attribute : attrInfo.getBasicAttributes() ) {
                        sql.append(",");
                        tmp[names.length] = attribute.getFieldName();
                        appendAttribute(tmp, fetch.getJoinType());
                        selectedAttributes.add(new SelectedAttribute(attribute, p));
                    }
                }
            }

            sql.append(" ");
            appendFrom(rootEntityInfo);
        }

        private void appendSelections() {
            List<? extends Selection<T>> selections = criteria.getSelections();
            Assert.state(selections != null && !selections.isEmpty(), "selections must not be empty");

            sql.append("SELECT ");
            boolean first = true;
            for ( Selection<T> selection : selections ) {
                if ( first ) {
                    first = false;
                } else {
                    sql.append(",");
                }
                AggregateFunctions aggregate = selection.getAggregateFunctions();
                if ( aggregate == null || aggregate == AggregateFunctions.NONE ) {
                    appendExpression(selection);
                } else {
                    sql.append(aggregate).append("(");
                    appendExpression(selection);
                    sql.append(")");
                }
            }

            sql.append(" ");
            appendFrom(rootEntityInfo);
        }

        private void appendColumnName(Attribute<?, ?> attribute) {
            sql.append("`").append(attribute.getColumnName()).append("`");
        }

        private void appendRootTableAlias() {
            appendRootTableAlias(sql);
        }

        private void appendRootTableAlias(StringBuilder sql) {
            sql.append(rootEntityInfo.getTableName()).append("0_");
        }

        private void appendWhereClause(WhereClause<T> whereClause) {
            if ( whereClause.isCompound() ) {
                appendCompoundWhereClause(whereClause);
            } else {
                appendNonCompoundWhereClause(whereClause);
            }
        }

        private void appendNonCompoundWhereClause(WhereClause<T> item) {
            Expression<T> expression = item.getExpression();
            if ( item.isNegate() ) {
                sql.append("NOT ");
            }
            appendExpression(expression);
            switch ( item.getConditionalOperator() ) {
                case EQUAL:
                    appendComparisonOperatorExpression(item, "=");
                    break;
                case GREATER_THAN:
                    appendComparisonOperatorExpression(item, ">");
                    break;
                case LESS_THAN:
                    appendComparisonOperatorExpression(item, "<");
                    break;
                case GREATER_THAN_OR_EQUAL_TO:
                    appendComparisonOperatorExpression(item, ">=");
                    break;
                case LESS_THAN_OR_EQUAL_TO:
                    appendComparisonOperatorExpression(item, "<=");
                    break;
                case BETWEEN: {
                    Iterator<?> iterator = ( (Iterable<?>) item.getParameter() ).iterator();
                    sql.append(" BETWEEN ");
                    appendSimpleParam(iterator.next());
                    sql.append(" AND ");
                    appendSimpleParam(iterator.next());
                    break;
                }
                case IN: {
                    sql.append(" IN(");
                    appendSqlParameter(item.getParameter());
                    sql.append(")");
                    break;
                }
                case LIKE:
                    appendComparisonOperatorExpression(item, " LIKE ");
                    break;
                case IS_NULL:
                    sql.append(" IS NULL");
                    break;
            }

        }

        private void appendComparisonOperatorExpression(WhereClause<T> item, String operator) {//比较运算符
            Object parameter = item.getParameter();
            sql.append(operator);
            appendSqlParameter(parameter);
        }

        private void appendSqlParameter(Object parameter) {
            if ( parameter instanceof Expression ) {
                //noinspection unchecked
                appendExpression((Expression<T>) parameter);
            } else if ( parameter instanceof Iterable ) {
                boolean fist = true;
                for ( Object arg : ( (Iterable<?>) parameter ) ) {
                    if ( fist ) {
                        fist = false;
                    } else {
                        sql.append(',');
                    }
                    appendSimpleParam(arg);
                }
            } else {
                appendSimpleParam(parameter);
            }
        }

        private void appendSimpleParam(Object arg) {
            if ( arg instanceof Number ) {
                sql.append(arg);
            } else {
                sql.append("?");
                args.add(arg);
            }
        }

        private void appendFunArg(Object[] parameter) {
            boolean first = true;
            for ( Object o : parameter ) {
                if ( first ) {
                    first = false;
                } else {
                    sql.append(",");
                }
                appendSqlParameter(o);
            }
        }

        private void appendExpression(Expression<T> expression) {
            Expression.Function function = expression.getFunction();
            function = function == null ? Expression.Function.NONE : function;
            switch ( function ) {
                case NONE:
                    appendAttribute(expression);
                    break;
                case ABS:
                    appendSingleParameterFunction(expression, "ABS");
                    break;
                case SUM: {
                    appendAttribute(expression.getSubexpression());
                    sql.append("+");
                    Object arg = expression.getArgs()[0];
                    appendSqlParameter(arg);
                    break;
                }
                case PROD: {
                    appendAttribute(expression.getSubexpression());
                    sql.append("*");
                    Object arg = expression.getArgs()[0];
                    appendFunArgs(arg);
                    break;
                }
                case DIFF: {
                    appendAttribute(expression.getSubexpression());
                    sql.append("-");
                    Object arg = expression.getArgs()[0];
                    appendFunArgs(arg);
                    break;
                }
                case QUOT: {
                    appendAttribute(expression.getSubexpression());
                    sql.append("/");
                    Object arg = expression.getArgs()[0];
                    appendFunArgs(arg);
                    break;
                }
                case MOD:
                    String mod = "MOD";
                    appendMultiParameterFunction(expression, mod);
                    break;
                case SQRT:
                    appendSingleParameterFunction(expression, "SQRT");
                    break;
                case CONCAT:
                    String concat = "CONCAT";
                    appendMultiParameterFunction(expression, concat);
                    break;
                case SUBSTRING:
                    appendMultiParameterFunction(expression, "SUBSTRING");
                    break;
                case TRIM:
                    appendSingleParameterFunction(expression, "TRIM");
                    break;
                case LOWER:
                    appendSingleParameterFunction(expression, "LOWER");
                    break;
                case UPPER:
                    appendSingleParameterFunction(expression, "UPPER");
                    break;
                case LENGTH:
                    appendSingleParameterFunction(expression, "UPPER");
                    break;
                case LOCATE:
                    sql.append("LOCATE").append("(");
                    appendFunArg(expression.getArgs());
                    sql.append(",");
                    appendAttribute(expression.getSubexpression());
                    sql.append(")");
                    break;
                case COALESCE:
                    appendMultiParameterFunction(expression, "COALESCE");
                    break;
                case NULLIF:
                    appendMultiParameterFunction(expression, "NULLIF");
                    break;
            }

        }

        private void appendMultiParameterFunction(Expression<T> expression, String funStr) {
            sql.append(funStr).append("(");
            appendAttribute(expression.getSubexpression());
            sql.append(",");
            appendFunArg(expression.getArgs());
            sql.append(")");
        }

        private void appendSingleParameterFunction(Expression<T> expression, String funStr) {
            sql.append(funStr).append("(");
            appendExpression(expression.getSubexpression());
            sql.append(")");
        }

        private void appendFunArgs(Object arg) {
            if ( arg instanceof Expression ) {
                //noinspection unchecked
                Expression<T> ex = (Expression<T>) arg;
                boolean lowPriority = ex.getFunction() == Expression.Function.SUM
                        || ex.getFunction() == Expression.Function.DIFF;
                if ( lowPriority ) {
                    sql.append("(");
                }
                appendExpression(ex);
                if ( lowPriority ) {
                    sql.append(")");
                }
            } else {
                appendSqlParameter(arg);
            }
        }

        private void appendAttribute(com.github.alittlehuang.data.query.specification.Attribute<T> attribute) {
            String[] names = attribute.getNames();
            appendAttribute(names, JoinType.LEFT);
        }

        private void appendAttribute(String[] names, JoinType joinType) {
            Attribute<?, ?> attr = rootEntityInfo.getAttribute(names[0]);
            if ( names.length > 1 ) {
                joinAttrs = joinAttrs == null ? new HashMap<>() : joinAttrs;
                JoinAttr joinAttr = null;
                for ( int i = 1; i < names.length; i++ ) {
                    JointKey key = new JointKey(joinAttr, attr);
                    if ( !joinAttrs.containsKey(key) ) {
                        joinAttrs.put(key, new JoinAttr(joinAttr, attr));
                    }
                    joinAttr = joinAttrs.get(key);
                    joinAttr.joinType = joinType;

                    EntityInformation attrInfo = EntityInformation.getInstance(attr.getFieldType());
                    attr = attrInfo.getAttribute(names[i]);
                    if ( !attr.isEntityType() ) {
                        joinAttr.appendAlias(sql);
                        sql.append('.');
                    }
                }
            } else {
                appendRootTableAlias();
                sql.append('.');
            }
            appendColumnName(attr);
        }

        private void appendCompoundWhereClause(WhereClause<T> whereClause) {

            int appendIndex = sql.length();

            List<? extends WhereClause<T>> items = whereClause.getCompoundItems();
            if ( items.size() == 1 ) {
                appendNonCompoundWhereClause(items.get(0));
            } else if ( !items.isEmpty() ) {
                boolean fist = true;
                Predicate.BooleanOperator pre = null;
                for ( WhereClause<T> item : items ) {
                    if ( fist ) {
                        fist = false;
                    } else {
                        Predicate.BooleanOperator operator = item.getBooleanOperator();
                        if ( pre == Predicate.BooleanOperator.OR && operator == Predicate.BooleanOperator.AND ) {
                            sql.insert(appendIndex, "(").append(")");
                        }
                        sql.append(operator == Predicate.BooleanOperator.OR ? " OR " : " AND ");
                        pre = operator;
                    }
                    boolean compound = item.isCompound() && item.getCompoundItems().size() > 1;
                    if ( compound ) {
                        sql.append("(");
                    }
                    appendWhereClause(item);

                    if ( compound ) {
                        sql.append(")");
                    }
                }
            }
        }

        class JoinAttr {
            Attribute<?, ?> attribute;
            EntityInformation attrInfo;
            JoinAttr parent;
            JoinType joinType;
            boolean appended = false;
            int index = joinAttrs.size();

            public JoinAttr(JoinAttr parent, Attribute<?, ?> attribute) {
                this.parent = parent;
                this.attribute = attribute;
                this.attrInfo = EntityInformation.getInstance(attribute.getFieldType());
            }

            void appendAlias(StringBuilder sql) {
                sql.append(attrInfo.getTableName())
                        .append(index)
                        .append("_");
            }
        }
    }
}
