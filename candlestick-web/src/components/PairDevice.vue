<template>test
  <v-card width="400" class="mx-auto mt-5">
      <v-card-title>
        <h1 class="display-1">Setup a new device</h1>
      </v-card-title>
      <v-card-text>
        <v-form>
          <v-text-field prepend-icon="mdi-tablet" label="Pairing Code" v-model="pairingCode" />
          <v-text-field prepend-icon="mdi-account-group" label="Admin Group" v-model="adminGroup" />
          <v-text-field
            prepend-icon="mdi-account-circle"
            label="Subscriber Id"
            v-model="subscriberId"
          />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-btn color="info" v-on:click="pair()">Pair</v-btn>
      </v-card-actions>
    </v-card>
</template>

<script>
import Vue from "vue";
import axios from "axios";
import VueAxios from "vue-axios";
Vue.use(VueAxios, axios);
axios.defaults.baseURL = "";
export default {
  name: "pair-device",
  data: () => {
    return {
      subscriberId: "",
      adminGroup: "",
      pairingCode: ""
    };
  },
  methods: {
    pair: function() {
      this.$http
        .post(
          "/api",
          {
            PairingCode: this.pairingCode,
            SubscriberId: this.adminGroup + "+" + this.subscriberId
          },
          { withCredentials: true, crossDomain: true }
        )
        .then(function(response) {
          console.log(response)
          this.pairingCode = ""
          this.subscriberId = ""
        })
        .catch(function(error) {
          console.log(error);
        });
    }
  }
};
</script>