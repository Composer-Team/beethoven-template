FROM ubuntu:22.04
RUN apt-get update
RUN apt-get install -y git gcc g++ cmake openjdk-17-jdk-headless vim automake binutils autoconf
RUN apt-get install -y gperf autoconf flex bison
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
USER beethoven
