/*
 * Copyright 2000-2019 United Planet GmbH, Freiburg Germany
 * All Rights Reserved.
 */
package de.uplanet.lucy.connectorapi.examples.mfiles

import org.odata4j.expression.*
import java.util.*

class MFilesFilterVisitor : PreOrderVisitor() {
    var lastPropertyName: Optional<String> = Optional.empty()
    var filterValueString : Optional<String> = Optional.empty()
    var filterValueLong : Optional<Long> = Optional.empty()
    var filterValueDate : Optional<Date> = Optional.empty()

    override fun afterDescend() {
        
    }

    override fun beforeDescend() {
        
    }
    
    override fun betweenDescend() {
        
    }

    override fun visit(p_value: String?) {

    }

    override fun visit(p0: OrderByExpression?) {
        
    }

    override fun visit(p0: OrderByExpression.Direction?) {
        
    }

    override fun visit(p0: AddExpression?) {
        
    }

    override fun visit(p0: AndExpression?) {
        
    }

    override fun visit(p0: BooleanLiteral?) {
        
    }

    override fun visit(p0: CastExpression?) {
        
    }

    override fun visit(p0: ConcatMethodCallExpression?) {
        
    }

    override fun visit(p0: DateTimeLiteral?) {
        filterValueDate = Optional.of(p0!!.value.toDate())
    }

    override fun visit(p0: DateTimeOffsetLiteral?) {
        filterValueDate = Optional.of(p0!!.value.toDate())
    }

    override fun visit(p0: DecimalLiteral?) {
        
    }

    override fun visit(p0: DivExpression?) {
        
    }

    override fun visit(p0: EndsWithMethodCallExpression?) {
        
    }

    override fun visit(p0: EntitySimpleProperty?) {
        lastPropertyName = Optional.of(p0!!.propertyName)
    }

    override fun visit(p0: EqExpression?) {
        
    }

    override fun visit(p0: GeExpression?) {
        
    }

    override fun visit(p0: GtExpression?) {
        
    }

    override fun visit(p0: GuidLiteral?) {
        
    }

    override fun visit(p0: BinaryLiteral?) {
        
    }

    override fun visit(p0: ByteLiteral?) {
        
    }

    override fun visit(p0: SByteLiteral?) {
        
    }

    override fun visit(p0: IndexOfMethodCallExpression?) {
        
    }

    override fun visit(p0: SingleLiteral?) {
        
    }

    override fun visit(p0: DoubleLiteral?) {
        
    }

    override fun visit(p0: IntegralLiteral?) {
        filterValueLong = Optional.of(p0!!.value.toLong())
    }

    override fun visit(p0: Int64Literal?) {
        filterValueLong = Optional.of(p0!!.value)
    }

    override fun visit(p0: IsofExpression?) {
        
    }

    override fun visit(p0: LeExpression?) {
        
    }

    override fun visit(p0: LengthMethodCallExpression?) {
        
    }

    override fun visit(p0: LtExpression?) {
        
    }

    override fun visit(p0: ModExpression?) {
        
    }

    override fun visit(p0: MulExpression?) {
        
    }

    override fun visit(p0: NeExpression?) {
        
    }

    override fun visit(p0: NegateExpression?) {
        
    }

    override fun visit(p0: NotExpression?) {
        
    }

    override fun visit(p0: NullLiteral?) {
        
    }

    override fun visit(p0: OrExpression?) {
        
    }

    override fun visit(p0: ParenExpression?) {
        
    }

    override fun visit(p0: BoolParenExpression?) {
        
    }

    override fun visit(p0: ReplaceMethodCallExpression?) {
        
    }

    override fun visit(p0: StartsWithMethodCallExpression?) {
        
    }

    override fun visit(p0: StringLiteral?) {
        filterValueString = Optional.of(p0!!.value)
    }

    override fun visit(p0: SubExpression?) {
        
    }

    override fun visit(p0: SubstringMethodCallExpression?) {
        
    }

    override fun visit(p0: SubstringOfMethodCallExpression?) {
        
    }

    override fun visit(p0: TimeLiteral?) {
        
    }

    override fun visit(p0: ToLowerMethodCallExpression?) {
        
    }

    override fun visit(p0: ToUpperMethodCallExpression?) {
        
    }

    override fun visit(p0: TrimMethodCallExpression?) {
        
    }

    override fun visit(p0: YearMethodCallExpression?) {
        
    }

    override fun visit(p0: MonthMethodCallExpression?) {
        
    }

    override fun visit(p0: DayMethodCallExpression?) {
        
    }

    override fun visit(p0: HourMethodCallExpression?) {
        
    }

    override fun visit(p0: MinuteMethodCallExpression?) {
        
    }

    override fun visit(p0: SecondMethodCallExpression?) {
        
    }

    override fun visit(p0: RoundMethodCallExpression?) {
        
    }

    override fun visit(p0: FloorMethodCallExpression?) {
        
    }

    override fun visit(p0: CeilingMethodCallExpression?) {
        
    }

    override fun visit(p0: AggregateAnyFunction?) {
        
    }

    override fun visit(p0: AggregateAllFunction?) {
        
    }
}