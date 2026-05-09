FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y git curl jq nodejs npm \
  && rm -rf /var/lib/apt/lists/*

# GitHub CLI
RUN curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg \
  | dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg \
  && echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" \
  | tee /etc/apt/sources.list.d/github-cli.list > /dev/null \
  && apt-get update && apt-get install -y gh && rm -rf /var/lib/apt/lists/*

# Build-args passed automatically by `sandcastle docker build-image`
# — they are set to the host user's UID/GID so bind-mounted files
#   and image-built files share the same owner without runtime chown.
ARG AGENT_UID=1000
ARG AGENT_GID=1000

# Create an 'agent' group with the target GID.
# If a group with that GID already exists in the base image (e.g. dialout=20),
# rename it to 'agent' instead of creating a duplicate.
# Then create the 'agent' user; --non-unique lets it share a UID if needed.
RUN groupadd -g "$AGENT_GID" agent 2>/dev/null \
    || groupmod -n agent "$(getent group "$AGENT_GID" | cut -d: -f1)" \
  && useradd --uid "$AGENT_UID" --gid "$AGENT_GID" --non-unique \
             --create-home --home-dir /home/agent --shell /bin/bash agent

USER ${AGENT_UID}:${AGENT_GID}

# Claude Code CLI — installed as the agent user so ~/.local/bin is writable
RUN curl -fsSL https://claude.ai/install.sh | bash
ENV PATH="/home/agent/.local/bin:$PATH"

WORKDIR /home/agent
ENTRYPOINT ["sleep", "infinity"]
