package org.metaborg.spt.cmd;

import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.core.editor.NullEditorRegistry;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.spt.core.SPTModule;

import com.google.inject.Singleton;

public class Module extends SPTModule {
    @Override protected void configure() {
        super.configure();

        bind(Runner.class).in(Singleton.class);
    }

    @Override protected void bindProject() {
        bind(SimpleProjectService.class).in(Singleton.class);
        bind(IProjectService.class).to(SimpleProjectService.class);
        bind(ISimpleProjectService.class).to(SimpleProjectService.class);
    }

    @Override protected void bindEditor() {
        bind(IEditorRegistry.class).to(NullEditorRegistry.class).in(Singleton.class);
    }
}
