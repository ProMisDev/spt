package org.metaborg.meta.lang.spt.strategies;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.tracing.IResolverService;
import org.metaborg.core.tracing.ITracingService;
import org.metaborg.core.tracing.Resolution;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.Term;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * <spt-resolve-reference(|filePath, langName, analyzedAST)> ref
 * 
 * Returns the list of all terms that this reference might resolve to.
 */
public class spt_resolve_reference_0_3 extends Strategy {

	public static final spt_resolve_reference_0_3 instance = new spt_resolve_reference_0_3();
	private spt_resolve_reference_0_3() {}
	private static final ILogger logger = LoggerUtils.logger(spt_reference_resolution_available_0_1.class);

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm refTerm, IStrategoTerm filePath, IStrategoTerm langName, IStrategoTerm analyzedAst) {
		final IContext metaborgContext = (IContext) context.contextObject();
		final Injector injector = metaborgContext.injector();
		final IResolverService<IStrategoTerm, IStrategoTerm> resolver = injector.getInstance(
				Key.get(new TypeLiteral<IResolverService<IStrategoTerm, IStrategoTerm>>(){})
		);
		final ILanguageService languageService = injector.getInstance(ILanguageService.class);
		final IResourceService resourceService = injector.getInstance(IResourceService.class);
		final ITermFactoryService termFactoryService = injector.getInstance(ITermFactoryService.class);
		final ITracingService<IStrategoTerm, IStrategoTerm, IStrategoTerm> tracingService = injector.getInstance(
				Key.get(new TypeLiteral<ITracingService<IStrategoTerm, IStrategoTerm, IStrategoTerm>>(){})
		);
		
		// Get the Language Under Test and its resource
		final ILanguage ilang = languageService.getLanguage(Term.asJavaString(langName));
		final ILanguageImpl lang = ilang.activeImpl();
		final FileObject sptFile = resourceService.resolve(Term.asJavaString(filePath));

		// Get the offset at which to try reference resolution
		final IToken leftToken = ImploderAttachment.getLeftToken(refTerm);
		if (!resolver.available(lang) || leftToken == null) {
			return null;
		}
		
		// Run the reference resolver
		final ITermFactory termFactory = termFactoryService.get(lang);
		final Resolution result;
		// TODO: is the 'previous' ParseResult allowed to be null?
		final AnalysisFileResult<IStrategoTerm, IStrategoTerm> mockAnalysis;
		try {
		    // HACK: use metaborgContext, it is the context used to analyze in analyze_fragment_0_2.
			mockAnalysis = new AnalysisFileResult<IStrategoTerm, IStrategoTerm>(
					analyzedAst, sptFile, metaborgContext, Iterables2.<IMessage>empty(), null
			);
			result = resolver.resolve(leftToken.getStartOffset(), mockAnalysis);
			logger.debug("Resolved {} to {}", refTerm, result);
		} catch (ContextException e) {
			final String err = "Couldn't get a context for reference resolution.";
			logger.error(err, e);
			return termFactory.makeAppl(termFactory.makeConstructor("Error", 1), termFactory.makeString(err));
		} catch (MetaborgException e) {
			final String err = "Failed to call reference resolver.";
			logger.error(err, e);
			return termFactory.makeAppl(termFactory.makeConstructor("Error", 1), termFactory.makeString(err));
		}
		
		if (result == null) {
			return termFactory.makeList();
		}
		
		// Retrieve the terms from the resolution result
		final Collection<IStrategoTerm> possibleResults = new ArrayList<IStrategoTerm>();
		for (ISourceLocation loc : result.targets) {
			for (IStrategoTerm possibleResult : tracingService.toAnalyzed(mockAnalysis, loc.region())) {
				possibleResults.add(possibleResult);
			}
		}
		
		return termFactory.makeList(possibleResults);
	}
}
