package play.templates;

import java.util.Map;

import javax.management.RuntimeErrorException;

import play.templates.BaseTemplate;
import play.templates.Template;

public class TemplateWrapper extends BaseTemplate {

    private BaseTemplate template;

    public TemplateWrapper(Template template) {
        super(template.name, template.source);
        if (template instanceof TemplateWrapper)
            throw new RuntimeException("Wrapper error");
        this.template = (BaseTemplate) template;
    }

    @Override
    public void loadPrecompiled() {
        template.loadPrecompiled();
    }

    @Override
    public boolean loadFromCache() {
        return template.loadFromCache();
    }

    @Override
    public void compile() {
        template.compile();
    }

    @Override
    protected String internalRender(Map<String, Object> args) {
        return template.internalRender(args);
    }

    void directLoad(byte[] code) throws Exception
    {
        template.directLoad(code);
    }

    Throwable cleanStackTrace(Throwable e)
    {
        return template.cleanStackTrace(e);
    }

    @Override
    public String render(Map<String, Object> args) {
        return template.render(args);
    }

    @Override
    public String render() {
        return template.render();
    }

    @Override
    public String getName() {
        return template.getName();
    }

}
