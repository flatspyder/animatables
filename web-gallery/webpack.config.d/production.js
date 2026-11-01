// Source maps remain available in development; static production hosting needs only the runtime.
if (config.mode === 'production') {
    config.devtool = false;
    config.output.clean = true;
}
