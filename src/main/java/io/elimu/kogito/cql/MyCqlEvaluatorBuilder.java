package io.elimu.kogito.cql;

import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;

public class MyCqlEvaluatorBuilder extends CqlEvaluatorBuilder {

	@Override
	protected TerminologyProvider decorate(TerminologyProvider terminologyProvider) {
		return new ImprovedCachingTerminologyProviderDecorator(terminologyProvider);
	}

}
