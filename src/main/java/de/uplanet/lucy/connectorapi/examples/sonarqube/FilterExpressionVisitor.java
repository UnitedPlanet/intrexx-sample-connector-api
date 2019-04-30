
package de.uplanet.lucy.connectorapi.examples.sonarqube;

import java.util.ArrayList;
import java.util.List;

import org.odata4j.expression.AddExpression;
import org.odata4j.expression.AggregateAllFunction;
import org.odata4j.expression.AggregateAnyFunction;
import org.odata4j.expression.AndExpression;
import org.odata4j.expression.BinaryLiteral;
import org.odata4j.expression.BoolParenExpression;
import org.odata4j.expression.BooleanLiteral;
import org.odata4j.expression.ByteLiteral;
import org.odata4j.expression.CastExpression;
import org.odata4j.expression.CeilingMethodCallExpression;
import org.odata4j.expression.ConcatMethodCallExpression;
import org.odata4j.expression.DateTimeLiteral;
import org.odata4j.expression.DateTimeOffsetLiteral;
import org.odata4j.expression.DayMethodCallExpression;
import org.odata4j.expression.DecimalLiteral;
import org.odata4j.expression.DivExpression;
import org.odata4j.expression.DoubleLiteral;
import org.odata4j.expression.EndsWithMethodCallExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.EqExpression;
import org.odata4j.expression.FloorMethodCallExpression;
import org.odata4j.expression.GeExpression;
import org.odata4j.expression.GtExpression;
import org.odata4j.expression.GuidLiteral;
import org.odata4j.expression.HourMethodCallExpression;
import org.odata4j.expression.IndexOfMethodCallExpression;
import org.odata4j.expression.Int64Literal;
import org.odata4j.expression.IntegralLiteral;
import org.odata4j.expression.IsofExpression;
import org.odata4j.expression.LeExpression;
import org.odata4j.expression.LengthMethodCallExpression;
import org.odata4j.expression.LtExpression;
import org.odata4j.expression.MinuteMethodCallExpression;
import org.odata4j.expression.ModExpression;
import org.odata4j.expression.MonthMethodCallExpression;
import org.odata4j.expression.MulExpression;
import org.odata4j.expression.NeExpression;
import org.odata4j.expression.NegateExpression;
import org.odata4j.expression.NotExpression;
import org.odata4j.expression.NullLiteral;
import org.odata4j.expression.OrExpression;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.expression.OrderByExpression.Direction;
import org.odata4j.expression.ParenExpression;
import org.odata4j.expression.PreOrderVisitor;
import org.odata4j.expression.ReplaceMethodCallExpression;
import org.odata4j.expression.RoundMethodCallExpression;
import org.odata4j.expression.SByteLiteral;
import org.odata4j.expression.SecondMethodCallExpression;
import org.odata4j.expression.SingleLiteral;
import org.odata4j.expression.StartsWithMethodCallExpression;
import org.odata4j.expression.StringLiteral;
import org.odata4j.expression.SubExpression;
import org.odata4j.expression.SubstringMethodCallExpression;
import org.odata4j.expression.SubstringOfMethodCallExpression;
import org.odata4j.expression.TimeLiteral;
import org.odata4j.expression.ToLowerMethodCallExpression;
import org.odata4j.expression.ToUpperMethodCallExpression;
import org.odata4j.expression.TrimMethodCallExpression;
import org.odata4j.expression.YearMethodCallExpression;

public final class FilterExpressionVisitor extends PreOrderVisitor {

	private final List<String> m_login;
	private String m_lastProp;
	private String m_email;
	private long m_newIssues = -1;
	
	public FilterExpressionVisitor()
	{
		m_login = new ArrayList<>();
	}
	
	public String getLastProp()
	{
		return m_lastProp;
	}

	public List<String> getLogin()
	{
		return m_login;
	}

	public String getEmail()
	{
		return m_email;
	}

	public long getNewIssues()
	{
		return m_newIssues;
	}

	@Override
	public void afterDescend()
	{

	}

	@Override
	public void beforeDescend()
	{

	}

	@Override
	public void betweenDescend()
	{

	}

	@Override
	public void visit(String p_value)
	{

	}

	@Override
	public void visit(OrderByExpression arg0)
	{

	}

	@Override
	public void visit(Direction arg0)
	{

	}

	@Override
	public void visit(AddExpression arg0)
	{

	}

	@Override
	public void visit(AndExpression arg0)
	{
		
	}

	@Override
	public void visit(BooleanLiteral arg0)
	{
		
	}

	@Override
	public void visit(CastExpression arg0)
	{

	}

	@Override
	public void visit(ConcatMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(DateTimeLiteral arg0)
	{

	}

	@Override
	public void visit(DateTimeOffsetLiteral arg0)
	{

	}

	@Override
	public void visit(DecimalLiteral arg0)
	{

	}

	@Override
	public void visit(DivExpression arg0)
	{

	}

	@Override
	public void visit(EndsWithMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(EntitySimpleProperty arg0)
	{
		m_lastProp = arg0.getPropertyName();
	}

	@Override
	public void visit(EqExpression arg0)
	{

	}

	@Override
	public void visit(GeExpression arg0)
	{

	}

	@Override
	public void visit(GtExpression arg0)
	{
		

	}

	@Override
	public void visit(GuidLiteral arg0)
	{

	}

	@Override
	public void visit(BinaryLiteral arg0)
	{

	}

	@Override
	public void visit(ByteLiteral arg0)
	{

	}

	@Override
	public void visit(SByteLiteral arg0)
	{

	}

	@Override
	public void visit(IndexOfMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(SingleLiteral arg0)
	{

	}

	@Override
	public void visit(DoubleLiteral arg0)
	{

	}

	@Override
	public void visit(IntegralLiteral arg0)
	{
		switch (m_lastProp) {
		case "newIssues": m_newIssues = arg0.getValue();
			break;
		default:
			break;
		}
	}

	@Override
	public void visit(Int64Literal arg0)
	{

	}

	@Override
	public void visit(IsofExpression arg0)
	{

	}

	@Override
	public void visit(LeExpression arg0)
	{

	}

	@Override
	public void visit(LengthMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(LtExpression arg0)
	{

	}

	@Override
	public void visit(ModExpression arg0)
	{

	}

	@Override
	public void visit(MulExpression arg0)
	{

	}

	@Override
	public void visit(NeExpression arg0)
	{
	
	}

	@Override
	public void visit(NegateExpression arg0)
	{

	}

	@Override
	public void visit(NotExpression arg0)
	{

	}

	@Override
	public void visit(NullLiteral arg0)
	{

	}

	@Override
	public void visit(OrExpression arg0)
	{

	}

	@Override
	public void visit(ParenExpression arg0)
	{

	}

	@Override
	public void visit(BoolParenExpression arg0)
	{

	}

	@Override
	public void visit(ReplaceMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(StartsWithMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(StringLiteral p_value)
	{
		switch (m_lastProp) {
		case "login":
			m_login.add(p_value.getValue());
			break;
		case "email":
			m_email = p_value.getValue();
			break;
		default:
			break;
		}
		m_lastProp = null;
		return;
	}

	@Override
	public void visit(SubExpression arg0)
	{

	}

	@Override
	public void visit(SubstringMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(SubstringOfMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(TimeLiteral arg0)
	{

	}

	@Override
	public void visit(ToLowerMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(ToUpperMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(TrimMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(YearMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(MonthMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(DayMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(HourMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(MinuteMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(SecondMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(RoundMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(FloorMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(CeilingMethodCallExpression arg0)
	{

	}

	@Override
	public void visit(AggregateAnyFunction arg0)
	{

	}

	@Override
	public void visit(AggregateAllFunction arg0)
	{

	}

}