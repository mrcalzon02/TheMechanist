# Runtime dependency jars

Place optional runtime dependency jars here. Launchers scan this directory recursively and add every `.jar` file to the Java classpath before running startup preflight or launching the client.

The first dependency family expected here is LWJGL for the future OpenGL/Doom renderer backend. The current bundled client still starts with the Java2D renderer if these jars are absent unless `MECHANIST_REQUIRE_LWJGL=true` or `-Dmechanist.requireLwjgl=true` is supplied.
