FROM tomcat:jre21

ARG WAR_FILE=*.war
COPY ${WAR_FILE} /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

# Copy a custom startup script
COPY startup.sh /usr/local/tomcat/bin/

# Make the script executable
RUN chmod +x /usr/local/tomcat/bin/startup.sh

# Use the custom startup script as the entry point
ENTRYPOINT ["/usr/local/tomcat/bin/startup.sh"]
