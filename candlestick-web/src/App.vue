<template>
  <v-app id="candlestick">
    <v-app-bar app clipped-left color="purple">
      <v-app-bar-nav-icon @click="drawer = !drawer"></v-app-bar-nav-icon>
      <span class="title ml-3 mr-5">Candlestick</span>
      <v-spacer></v-spacer>
    </v-app-bar>
    <v-navigation-drawer v-model="drawer" app clipped color="grey lighten-4">
      <v-list dense class="grey lighten-4">
        <template v-for="(item, i) in items">
          <v-row v-if="item.heading" :key="i" align="center">
            <v-col cols="6">
              <v-subheader v-if="item.heading">{{ item.heading }}</v-subheader>
            </v-col>
            <v-col cols="6" class="text-right">
              <v-btn small text>edit</v-btn>
            </v-col>
          </v-row>
          <v-divider
            v-else-if="item.divider"
            :key="i"
            dark
            class="my-4"
          ></v-divider>
          <v-list-item v-else :key="i" link>
            <v-list-item-action>
              <v-icon>{{ item.icon }}</v-icon>
            </v-list-item-action>
            <v-list-item-content>
              <v-list-item-title class="grey--text">{{
                item.text
              }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </template>
      </v-list>
    </v-navigation-drawer>
    <v-main>
      <v-container fluid class="grey lighten-4 fill-height">
        <v-row justify="center" align="center">
          <v-col class="shrink"></v-col>
        </v-row>
      </v-container>
    </v-main>
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
  </v-app>
</template>
<script>
export default {
  props: {
    source: String
  },
  data: () => ({
    drawer: null,
    items: [{ icon: "add", text: "Add new device" }]
  })
};
</script>
<style>
#keep .v-navigation-drawer__border {
  display: none;
}
</style>
