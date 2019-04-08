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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

        StringBuilder sql = new StringBuilder();

        public Builder(Criteria<T> criteria) {
            this.criteria = criteria;
            entityInfo = EntityInformation.getInstance(criteria.getJavaType());
            whereClause = criteria.getWhereClause();
        }

        SqlBuilder.PrecompiledSql buildListResult() {
            sql.append(selectFrom());
            appendWhere();
            return toPrecompiledSql();
        }

        private void appendWhere() {
            if ( !whereClause.isCompound() || !whereClause.getCompoundItems().isEmpty() ) {
                sql.append(" WHERE ");
                appendWhere(whereClause);
            }
        }

        SqlBuilder.PrecompiledSql buildCount() {
            sql.append("SELECT COUNT(1) FROM `").append(entityInfo.getTableName()).append('`');
            appendWhere();
            return toPrecompiledSql();
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


        private static final Map<Class, String> SELECT_FROM_CLASS_SQL = new ConcurrentHashMap<>();

        private String selectFrom() {
            Class<?> clazz = criteria.getJavaType();
            return SELECT_FROM_CLASS_SQL.computeIfAbsent(clazz, javaType -> {
                StringBuilder builder = new StringBuilder("SELECT");
                EntityInformation<?, ?> entityInformation = EntityInformation.getInstance(clazz);

                boolean first = true;
                for ( Attribute<?, ?> attribute : entityInformation.getAllAttributes() ) {
                    if ( first ) {
                        builder.append(" ");
                        first = false;
                    } else {
                        builder.append(",");
                    }
                    builder.append("`").append(attribute.getColumnName()).append("`");
                }
                builder.append(" FROM ")
                        .append("`")
                        .append(entityInformation.getTableName())
                        .append("`");

                return builder.toString();
            });

        }

        private void appendWhere(WhereClause<T> whereClause) {
            if ( whereClause.isCompound() ) {
                recursiveBuild(whereClause);
            } else {
                buildListResult(whereClause);
            }
        }

        private void buildListResult(WhereClause<T> item) {
            Expression<T> expression = item.getExpression();
            if ( item.isNegate() ) {
                sql.append("NOT ");
            }
            appendExp(expression);
            switch ( item.getConditionalOperator() ) {
                case EQUAL:
                    appendParameter(item, "=");
                    break;
                case GREATER_THAN:
                    appendParameter(item, ">");
                    break;
                case LESS_THAN:
                    appendParameter(item, "<");
                    break;
                case GREATER_THAN_OR_EQUAL_TO:
                    appendParameter(item, ">=");
                    break;
                case LESS_THAN_OR_EQUAL_TO:
                    appendParameter(item, "<=");
                    break;
                case BETWEEN: {
                    Iterator<?> iterator = ( (Iterable<?>) item.getParameter() ).iterator();
                    sql.append(" BETWEEN ");
                    appendSingleParam(iterator.next());
                    sql.append(" AND ");
                    appendSingleParam(iterator.next());
                    break;
                }
                case IN: {
                    sql.append(" IN(");
                    appendParameter(item.getParameter());
                    sql.append(")");
                    break;
                }
                case LIKE:
                    appendParameter(item, " LIKE ");
                    break;
                case IS_NULL:
                    sql.append(" IS NULL");
                    break;
            }

        }

        private void appendParameter(WhereClause<T> item, String operator) {
            Object parameter = item.getParameter();
            sql.append(operator);
            appendParameter(parameter);
        }

        private void appendParameter(Object parameter) {
            if ( parameter instanceof Expression ) {
                //noinspection unchecked
                appendExp((Expression<T>) parameter);
            } else if ( parameter instanceof Iterable ) {
                boolean fist = true;
                for ( Object arg : ( (Iterable<?>) parameter ) ) {
                    if ( fist ) {
                        fist = false;
                    } else {
                        sql.append(',');
                    }
                    appendSingleParam(arg);
                }
            } else {
                appendSingleParam(parameter);
            }
        }

        private void appendSingleParam(Object arg) {
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
                appendParameter(o);
            }
        }

        private void appendExp(Expression<T> expression) {
            Expression.Function function = expression.getFunction();
            function = function == null ? Expression.Function.NONE : function;
            switch ( function ) {
                case NONE:
                    appendSimpleExp(expression);
                    break;
                case ABS:
                    appendFunSingleArg(expression, "ABS");
                    break;
                case SUM: {
                    appendSimpleExp(expression.getSubexpression());
                    sql.append("+");
                    Object arg = expression.getArgs()[0];
                    appendParameter(arg);
                    break;
                }
                case PROD: {
                    appendSimpleExp(expression.getSubexpression());
                    sql.append("*");
                    Object arg = expression.getArgs()[0];
                    appendFunArgs(arg);
                    break;
                }
                case DIFF: {
                    appendSimpleExp(expression.getSubexpression());
                    sql.append("-");
                    Object arg = expression.getArgs()[0];
                    appendFunArgs(arg);
                    break;
                }
                case QUOT: {
                    appendSimpleExp(expression.getSubexpression());
                    sql.append("/");
                    Object arg = expression.getArgs()[0];
                    appendFunArgs(arg);
                    break;
                }
                case MOD:
                    String mod = "MOD";
                    appendManyArgFun(expression, mod);
                    break;
                case SQRT:
                    appendFunSingleArg(expression, "SQRT");
                    break;
                case CONCAT:
                    String concat = "CONCAT";
                    appendManyArgFun(expression, concat);
                    break;
                case SUBSTRING:
                    appendManyArgFun(expression, "SUBSTRING");
                    break;
                case TRIM:
                    appendFunSingleArg(expression, "TRIM");
                    break;
                case LOWER:
                    appendFunSingleArg(expression, "LOWER");
                    break;
                case UPPER:
                    appendFunSingleArg(expression, "UPPER");
                    break;
                case LENGTH:
                    appendFunSingleArg(expression, "UPPER");
                    break;
                case LOCATE:
                    sql.append("LOCATE").append("(");
                    appendFunArg(expression.getArgs());
                    sql.append(",");
                    appendSimpleExp(expression.getSubexpression());
                    sql.append(")");
                    break;
                case COALESCE:
                    appendManyArgFun(expression, "IFNULL");
                    break;
                case NULLIF:
                    appendManyArgFun(expression, "NULLIF");
                    break;
            }

        }

        private void appendManyArgFun(Expression<T> expression, String funName) {
            sql.append(funName).append("(");
            appendSimpleExp(expression.getSubexpression());
            sql.append(",");
            appendFunArg(expression.getArgs());
            sql.append(")");
        }

        private void appendFunSingleArg(Expression<T> expression, String funStr) {
            sql.append(funStr).append("(");
            appendExp(expression.getSubexpression());
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
                appendExp(ex);
                if ( lowPriority ) {
                    sql.append(")");
                }
            } else {
                appendParameter(arg);
            }
        }

        private void appendSimpleExp(Expression<T> expression) {
            String[] names = expression.getNames(entityInfo.getJavaType());
            Attribute<T, Object> attribute = entityInfo.getAttribute(names[0]);
            sql.append(attribute.getColumnName());
            if ( names.length != 1 ) {
                for ( int i = 1; i < names.length; i++ ) {
                    EntityInformation<Object, Object> info = EntityInformation.getInstance(attribute.getFieldType());
                    sql.append(".").append(info.getAttribute(names[i]).getColumnName());
                }
            }
        }

        private void recursiveBuild(WhereClause<T> whereClause) {

            int appendIndex = sql.length();

            List<? extends WhereClause<T>> items = whereClause.getCompoundItems();
            if ( items.size() == 1 ) {
                buildListResult(items.get(0));
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
                    appendWhere(item);

                    if ( compound ) {
                        sql.append(")");
                    }
                }
            }
        }

    }
}
