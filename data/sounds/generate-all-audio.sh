#!/bin/bash

# Copyright 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This script regenerates AllAudio.mk based on the content of the other
# makefiles.

# It needs to be run from its location in the source tree.

cat > AllAudio.mk << EOF
# Copyright 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := frameworks/base/data/sounds

PRODUCT_COPY_FILES += \\
EOF

recurse() {
 for i in "$1"/*;do
    if [ -d "$i" ]; then
        recurse "$i"        
    elif [ -f "$i" -a "$(dirname $i)" != "." ]; then
        folder=$(basename $(dirname $i))
        name=$(basename $i)
        item="\t\$(LOCAL_PATH)/$folder/$name:system/media/audio/$folder/$name \\"
        echo -e "$item"
        echo -e "$item" >> AllAudio.mk
    fi
 done
}

recurse .
