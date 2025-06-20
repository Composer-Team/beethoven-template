FROM ubuntu:22.04
RUN apt-get update
RUN apt-get install -y git g++ gcc cmake openjdk-17-jdk-headless  \
                        vim automake binutils autoconf python3.11 \
                        gperf autoconf flex bison rsync
RUN git clone https://github.com/Composer-Team/Beethoven-Software && \
    cd Beethoven-Software && \
    make install_swlib SUDO= 
ENV BEETHOVEN_PATH=/workspaces/hw_build
RUN mkdir -p Beethoven-Software/build && \
    cd Beethoven-Software/build && \
    rm -rf * && \
    cmake .. -DPLATFORM=discrete && \
    make -j && \
    make install
RUN git clone https://github.com/steveicarus/iverilog.git
RUN cd iverilog && \
    sh autoconf.sh && \
    ./configure && \
    make -j 8 && \
    make install

RUN adduser beethoven
COPY aws/* /home/beethoven/.ssh/
COPY aws ~/.ssh
RUN chown beethoven:beethoven /home/beethoven/.ssh/id_ed25519
RUN chown beethoven:beethoven /home/beethoven/.ssh/id_ed25519.pub
RUN chown beethoven:beethoven /home/beethoven/.ssh

# RUN apt-get install procmail
USER beethoven

