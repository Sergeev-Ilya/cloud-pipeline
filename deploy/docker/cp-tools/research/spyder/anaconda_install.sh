#!/bin/bash

# Copyright 2017-2019 EPAM Systems, Inc. (https://www.epam.com/)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

_ANACONDA_HOME=$1
_ANACONDA_VERSION=$2

setfacl -d -m g::rwx $_ANACONDA_HOME

wget -q "https://repo.anaconda.com/archive/Anaconda${_ANACONDA_VERSION}-Linux-x86_64.sh" -O /tmp/Anaconda_Install.sh && \
    bash /tmp/Anaconda_Install.sh -f -b -p $ANACONDA_HOME && \
    rm -f /tmp/Anaconda_Install.sh

echo ". $_ANACONDA_HOME/etc/profile.d/conda.sh" >> /etc/bash.bashrc && \
echo ". $_ANACONDA_HOME/etc/profile.d/conda.sh" >> /etc/profile && \
echo "conda activate" >> /etc/bash.bashrc && \
echo "conda activate" >> /etc/profile
