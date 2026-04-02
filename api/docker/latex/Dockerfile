FROM debian:bookworm-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       texlive-latex-base \
       texlive-latex-recommended \
       texlive-latex-extra \
         texlive-lang-german \
       texlive-fonts-recommended \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /work
ENTRYPOINT ["pdflatex"]
