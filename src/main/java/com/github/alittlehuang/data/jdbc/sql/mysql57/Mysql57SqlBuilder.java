package com.github.alittlehuang.data.jdbc.sql.mysql57;

import com.github.alittlehuang.data.jdbc.metamodel.Attribute;
import com.github.alittlehuang.data.jdbc.metamodel.EntityInformation;
import com.github.alittlehuang.data.jdbc.sql.SqlBuilder;
import com.github.alittlehuang.data.query.specification.Criteria;
import com.github.alittlehuang.data.query.specification.Expression;
import com.github.alittlehuang.data.query.specification.Selection;
import com.github.alittlehuang.data.query.specification.WhereClause;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Mysql57SqlBuilder implements SqlBuilder {

    @Override
    public SqlBuilder.PrecompiledSql count(Criteria<?> criteria) {
        return new Builder<>(criteria).buildCount();
    }

    @Override
    public SqlBuilder.PrecompiledSql exists(Criteria<?> criteria) {
        return null;
    }

    @Override
    public SqlBuilder.PrecompiledSql listResult(Criteria<?> criteria) {
        return new Builder<>(criteria).buildListResult();
    }

    @Override
    public SqlBuilder.PrecompiledSql ListObjects(Criteria<?> criteria) {
        List<? extends Selection<?>> selections = criteria.getSelections();
        if ( selections == null || selections.isEmpty() ) {
            throw new RuntimeException("the selections must not be empty");
        }
        return null;
    }

    private static class Builder<T> {

        private Criteria<?> criteria;
        EntityInformation<T, ?> entityInfo;
        WhereClause<T> whereClause;
        List<Object> args = new ArrayList<>();

        StringBuilder sql;

        public Builder(Criteria<T> criteria) {
            this.criteria = criteria;
            entityInfo = EntityInformation.getInstance(criteria.getJavaType());
            whereClause = criteria.getWhereClause();
        }

        SqlBuilder.PrecompiledSql buildListResult() {
            sql = new StringBuilder();
            appendSelectFromEntity();
            appendWhereClause();
            return toPrecompiledSql();
        }

        private void appendWhereClause() {
            if ( whereClause == null || !whereClause.isCompound() || !whereClause.getCompoundItems().isEmpty() ) {
                sql.append(" WHERE ");
                appendWhereClause(whereClause);
            }
        }

        SqlBuilder.PrecompiledSql buildCount() {
            sql = new StringBuilder();
            sql.append("SELECT COUNT(1) ");
            appendFrom(entityInfo);
            appendWhereClause();
            return toPrecompiledSql();
        }

        private void appendFrom(EntityInformation entityInfo) {
            sql.append("FROM `")
                    .append(entityInfo.getTableName())
                    .append("` ");
            appendTableAlias(entityInfo);
        }

        private PrecompiledSql toPrecompiledSql() {
            return new PrecompiledSql() {
                @Override
                public String getSql() {
                    return sql.toString();
                }

                @Override
                public List<Object> getArgs() {
                    return args;
                }
            };
        }


        private void appendSelectFromEntity() {
            Class<?> clazz = criteria.getJavaType();
            sql.append("SELECT");
            EntityInformation<?, ?> entityInformation = EntityInformation.getInstance(clazz);

            boolean first = true;
            for ( Attribute<?, ?> attribute : entityInformation.getAllAttributes() ) {
                if ( first ) {
                    sql.append(" ");
                    first = false;
                } else {
                    sql.append(",");
                }
                appendColumnName(sql, attribute);
            }
            sql.append(" ");
            appendFrom(entityInfo);
        }

        private void appendColumnName(StringBuilder sql, Attribute<?, ?> attribute) {
            EntityInformation<?, Object> entityInformation = EntityInformation.getInstance(attribute.getEntityType());
            appendTableAlias(entityInformation);
            sql.append(".`").append(attribute.getColumnName()).append("`");
        }

        private void appendTableAlias(EntityInformation entityInformation) {
            sql.append("`").append(entityInformation.getTableName()).append("_").append("`");
        }

        private void appendColumnName(Attribute<?, ?> attribute) {
            appendColumnName(sql, attribute);
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
                    appendComputation(expression);
                    break;
                case ABS:
                    appendSingleParameterFunction(expression, "ABS");
                    break;
                case SUM: {
                    appendComputation(expression.getSubexpression());
                    sql.append("+");
                    Object arg = expression.getArgs()[0];
                    appendSqlParameter(arg);
                    break;
                }
                case PROD: {
                    appendComputation(expression.getSubexpression());
                    sql.append("*");
                    Object arg = expression.getArgs()[0];
                    appendFunArgs(arg);
                    break;
                }
                case DIFF: {
                    appendComputation(expression.getSubexpression());
                    sql.append("-");
                    Object arg = expression.getArgs()[0];
                    appendFunArgs(arg);
                    break;
                }
                case QUOT: {
                    appendComputation(expression.getSubexpression());
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
                    appendComputation(expression.getSubexpression());
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
            appendComputation(expression.getSubexpression());
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

        private void appendComputation(Expression<T> expression) {
            String[] names = expression.getNames(entityInfo.getJavaType());
            Attribute<T, Object> attribute = entityInfo.getAttribute(names[0]);
            appendColumnName(attribute);
            if ( names.length != 1 ) {
                for ( int i = 1; i < names.length; i++ ) {
                    EntityInformation<Object, Object> info = EntityInformation.getInstance(attribute.getFieldType());
                    sql.append(".");
                    appendColumnName(info.getAttribute(names[i]));
                }
            }
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

    }
}
