Food Details
// src/screens/FoodDetails.js
    import React, {Component} from 'react';
    import {View, Button, Alert} from 'react-native';
    
    import NavHeaderRight from '../components/NavHeaderRight';
    import PageCard from '../components/PageCard';
    
    import {AppContext} from '../../GlobalContext';
    
    class FoodDetails extends Component {
      static navigationOptions = ({navigation}) => {
        return {
          title: navigation.getParam('item').name.substr(0, 12) + '...',
          headerRight: <NavHeaderRight />,
        };
      };
    
      static contextType = AppContext; // set this.context to the global app context
    
      state = {
        qty: 1,
      };
    
      constructor(props) {
        super(props);
        const {navigation} = this.props;
        this.item = navigation.getParam('item'); // get the item passed from the FoodList screen
      }
    
      qtyChanged = value => {
        const nextValue = Number(value);
        this.setState({qty: nextValue});
      };
    
      addToCart = (item, qty) => {
        // prevent the customer from adding items with more than one restaurant ids
        const item_id = this.context.cart_items.findIndex(
          el => el.restaurant.id !== item.restaurant.id,
        );
        if (item_id === -1) {
          Alert.alert(
            'Added to basket',
            `${qty} ${item.name} was added to the cart.`,
          );
          this.context.addToCart(item, qty); // call addToCart method from global app context
        } else {
          Alert.alert(
            'Cannot add to cart',
            'You can only order from one restaurant in one order.',
          );
        }
      };
    
      render() {
        const {qty} = this.state;
        return (
          <PageCard
            item={this.item}
            qty={qty}
            qtyChanged={this.qtyChanged}
            addToCart={this.addToCart}
          />
        );
      }
    }
    
    export default FoodDetails;

GlobalContext
// GlobalContext.js
    import React from 'react';
    import {withNavigation} from 'react-navigation';
    export const AppContext = React.createContext({}); // create a context
    
    export class AppContextProvider extends React.Component {
      state = {
        cart_items: [],
    
        user_id: 'ramkumar',
        user_name: Ram Kumar',
      };
    
      constructor(props) {
        super(props);
      }
    
      addToCart = (item, qty) => {
        let found = this.state.cart_items.filter(el => el.id === item.id);
        if (found.length == 0) {
          this.setState(prevState => {
            return {cart_items: prevState.cart_items.concat({...item, qty})};
          });
        } else {
          this.setState(prevState => {
            const other_items = prevState.cart_items.filter(
              el => el.id !== item.id,
            );
            return {
              cart_items: [...other_items, {...found[0], qty: found[0].qty + qty}],
            };
          });
        }
      };
    
      // next: add render()
    }
render() {
      return (
        <AppContext.Provider
          value={{
            ...this.state,
            addToCart: this.addToCart,
          }}>
          {this.props.children}
        </AppContext.Provider>
      );
    }
export const withAppContextProvider = ChildComponent => props => (
      <AppContextProvider>
        <ChildComponent {...props} />
      </AppContextProvider>
    );
Index
// index.js
    import {AppRegistry} from 'react-native';
    import App from './App';
    import {name as appName} from './app.json';
    import {withAppContextProvider} from './GlobalContext'; // add this
    
    AppRegistry.registerComponent(appName, () => withAppContextProvider(App)); // AppContextProvider to wrap the app
 import {AppContext} from '../../GlobalContext';
class FoodDetails extends Component {
      static contextType = AppContext; 
      // ...
    }
this.context.cart_items;
    this.context.addToCart(item, qty);

Order Summary
// src/screens/OrderSummary.js
    import React, {Component} from 'react';
    import {
      View,
      Text,
      Button,
      TouchableOpacity,
      FlatList,
      StyleSheet,
    } from 'react-native';
    import MapView from 'react-native-maps';
    import RNGooglePlaces from 'react-native-google-places';
    import {check, request, PERMISSIONS, RESULTS} from 'react-native-permissions';
    
    import Geolocation from 'react-native-geolocation-service';
    import Geocoder from 'react-native-geocoding';
    import Config from 'react-native-config';
    
    import {AppContext} from '../../GlobalContext';
    
    import getSubTotal from '../helpers/getSubTotal';
    
    import {regionFrom} from '../helpers/location';
    
    const GOOGLE_API_KEY = Config.GOOGLE_API_KEY;
    
    Geocoder.init(GOOGLE_API_KEY);
class OrderSummary extends Component {
      static navigationOptions = {
        title: 'Order Summary',
      };
    
      static contextType = AppContext;
    
      state = {
        customer_address: '',
        customer_location: null,
        restaurant_address: '',
        restaurant_location: null,
      };
    
      // next: add componentDidMount
    }
let location_permission = await check(
      PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION,
    );
    
    if (location_permission === 'denied') {
      location_permission = await request(
        PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION,
      );
    }
    
    if (location_permission == 'granted') {
      Geolocation.getCurrentPosition(
        async position => {
          const geocoded_location = await Geocoder.from(
            position.coords.latitude,
            position.coords.longitude,
          );
    
          let customer_location = regionFrom(
            position.coords.latitude,
            position.coords.longitude,
            position.coords.accuracy,
          );
    
          this.setState({
            customer_address: geocoded_location.results[0].formatted_address,
            customer_location,
          });
        },
        error => {
          console.log(error.code, error.message);
        },
        {
          enableHighAccuracy: true,
          timeout: 15000,
          maximumAge: 10000,
        },
      );
    }
    
    // add render()
render() {
      const subtotal = getSubTotal(this.context.cart_items);
      const {customer_address, customer_location} = this.state;
    
      return (
        <View style={styles.wrapper}>
          <View style={styles.addressSummaryContainer}>
            {customer_location && (
              <View style={styles.mapContainer}>
                <MapView style={styles.map} initialRegion={customer_location} />
              </View>
            )}
    
            <View style={styles.addressContainer}>
              {customer_address != '' &&
                this.renderAddressParts(customer_address)}
    
              <TouchableOpacity
                onPress={() => {
                  this.openPlacesSearchModal();
                }}>
                <View style={styles.linkButtonContainer}>
                  <Text style={styles.linkButton}>Change location</Text>
                </View>
              </TouchableOpacity>
            </View>
          </View>
          <View style={styles.cartItemsContainer}>
            <FlatList
              data={this.context.cart_items}
              renderItem={this.renderCartItem}
              keyExtractor={item => item.id.toString()}
            />
          </View>
    
          <View style={styles.lowerContainer}>
            <View style={styles.spacerBox} />
    
            {subtotal > 0 && (
              <View style={styles.paymentSummaryContainer}>
                <View style={styles.endLabelContainer}>
                  <Text style={styles.priceLabel}>Subtotal</Text>
                  <Text style={styles.priceLabel}>Booking fee</Text>
                  <Text style={styles.priceLabel}>Total</Text>
                </View>
    
                <View>
                  <Text style={styles.price}>${subtotal}</Text>
                  <Text style={styles.price}>$5</Text>
                  <Text style={styles.price}>${subtotal + 5}</Text>
                </View>
              </View>
            )}
          </View>
    
          {subtotal == 0 && (
            <View style={styles.messageBox}>
              <Text style={styles.messageBoxText}>Your cart is empty</Text>
            </View>
          )}
    
          {subtotal > 0 && (
            <View style={styles.buttonContainer}>
              <Button
                onPress={() => this.placeOrder()}
                title="Place Order"
                color="#c53c3c"
              />
            </View>
          )}
        </View>
      );
    }
To render the individual parts of the address, we will use renderAddresssParts().

renderAddressParts = customer_address => {
      return customer_address.split(',').map((addr_part, index) => {
        return (
          <Text key={index} style={styles.addressText}>
            {addr_part}
          </Text>
        );
      });
    };
Clicking on the ‘change location’ button will allow them to pick a place with React Native Google Places library. It directly provides the name of the place, so Geocoding is not required.

openPlacesSearchModal = async () => {
      try {
        const place = await RNGooglePlaces.openAutocompleteModal(); // open modal for picking a place
    
        const customer_location = regionFrom(
          place.location.latitude,
          place.location.longitude,
          16, // accuracy
        );
    
        this.setState({
          customer_address: place.address,
          customer_location,
        });
      } catch (err) {
        console.log('err: ', err);
      }
    };
The renderCartItem() method.

renderCartItem = ({item}) => {
      return (
        <View style={styles.cartItemContainer}>
          <View>
            <Text style={styles.priceLabel}>
              {item.qty}x {item.name}
            </Text>
          </View>
          <View>
            <Text style={styles.price}>${item.price}</Text>
          </View>
        </View>
      );
    };
The placeOrder() method will extract customer coordinates and addresses along with restaurant location and address. Since the user can only order from one restaurant, we get the first item, and it will be the same for the other items in the cart. We will pass the data as a navigation param to the TrackOrder screen as soon as we get it.

placeOrder = () => {
      const {customer_location, customer_address} = this.state;
    
      const {
        address: restaurant_address,
        location: restaurant_location,
      } = this.context.cart_items[0].restaurant; // get the location and address of the restaurant
    
      this.props.navigation.navigate('TrackOrder', {
        customer_location,
        restaurant_location,
        customer_address,
        restaurant_address,
      });
    };
Track Order
Now, we move to the TrackOrder screen. A map interface on this screen will help the users keep track of their orders. The markers for the driver’s location, restaurant location, and customer’s location will be seen here. Here are the packages you need to import.

// src/screens/TrackOrder.js
    import React, {Component} from 'react';
    import {View, Text, Button, Alert, StyleSheet} from 'react-native';
    
    import MapView from 'react-native-maps';
    import Geolocation from 'react-native-geolocation-service';
    import MapViewDirections from 'react-native-maps-directions';
    import Pusher from 'pusher-js/react-native';
    
    import Config from 'react-native-config';
    
    const CHANNELS_APP_KEY = Config.CHANNELS_APP_KEY;
    const CHANNELS_APP_CLUSTER = Config.CHANNELS_APP_CLUSTER;
    const CHANNELS_AUTH_SERVER = 'YOUR NGROK HTTPS URL/pusher/auth';
    
    const GOOGLE_API_KEY = Config.GOOGLE_API_KEY;
    
    import {regionFrom} from '../helpers/location';
    import {AppContext} from '../../GlobalContext';
Now, add the array with the status messages for the order. 

const orderSteps = [
      'Looking for a driver',
      'Driver is on their way to pick up the order',
      'The order is on the way',
      'Your order has been delivered',
    ];
After that, create the component class. The state needs to be initialized.

class TrackOrder extends Component {
      static navigationOptions = ({navigation}) => {
        return {
          title: 'Track Order',
        };
      };
    
      static contextType = AppContext;
    
      state = {
        isSearching: true, // If the app is still searching for a driver
        hasDriver: false, // If there's already a driver allotted to the order
        driverLocation: null, // the coordinates of the driver's location
        orderStatusText: orderSteps[0], // display the first message by default
      };
    
      // next: add the constructor()
    }
Get the navigation params that you passed earlier on OrderSummary. Then, initialize the instance variables.

constructor(props) {
      super(props);
    
      this.customer_location = this.props.navigation.getParam(
        'customer_location',
      ); // customer's location
      this.restaurant_location = this.props.navigation.getParam(
        'restaurant_location',
      );
    
      this.customer_address = this.props.navigation.getParam('customer_address');
      this.restaurant_address = this.props.navigation.getParam(
        'restaurant_address',
      );
    
      this.available_drivers_channel = null; // the pusher channel where all customers and drivers are subscribed to
      this.user_ride_channel = null; // the pusher channel exclusive to the driver and customer in a given order
      this.pusher = null; // pusher client
    }
    
    // next: add componentDidMount()
To look for available drivers, we need to initialize the Pusher client and subscribe to the channel on componentDidMount(). When subscribed, it will trigger a request for a driver. This event will have all the relevant information we got from the screen before.

componentDidMount() {
      this.setState({
        isSearching: true, 
      });
    
      this.pusher = new Pusher(CHANNELS_APP_KEY, {
        authEndpoint: CHANNELS_AUTH_SERVER,
        cluster: CHANNELS_APP_CLUSTER,
        encrypted: true,
      });
    
      this.available_drivers_channel = this.pusher.subscribe(
        'private-available-drivers',
      );
    
      this.available_drivers_channel.bind('pusher:subscription_succeeded', () => {
        // request to all drivers
        setTimeout(() => {
          this.available_drivers_channel.trigger('client-driver-request', {
            customer: {username: this.context.user_id},
            restaurant_location: this.restaurant_location,
            customer_location: this.customer_location,
            restaurant_address: this.restaurant_address,
            customer_address: this.customer_address,
          });
        }, 2000);
      });
    
      // subscribe to user-ride channel
    }
Then, we will subscribe to the current user’s own channel. It will become a bridge for communication between the customer and the driver who responded to the request.

Here, we listen to the driver’s end for the client-driver-response event. When it happens, a yes or no response will be sent. If the customer hasn’t been allocated a driver yet, then ‘yes’ will be sent. If the driver receives ‘yes,’ he/she will trigger a client-fount-driver from their side, which will be received by the customer, and the driver’s location will be stated.

this.user_ride_channel = this.pusher.subscribe(
      'private-ride-' + this.context.user_id,
    );
    
    this.user_ride_channel.bind('client-driver-response', data => {
      // customer responds to driver's response
      this.user_ride_channel.trigger('client-driver-response', {
        response: this.state.hasDriver ? 'no' : 'yes',
      });
    });
    
    this.user_ride_channel.bind('client-found-driver', data => {
      // found driver, the customer will have no say about this.
      const driverLocation = regionFrom(
        data.location.latitude,
        data.location.longitude,
        data.location.accuracy,
      );
    
      this.setState({
        hasDriver: true,
        isSearching: false,
        driverLocation,
      });
    
      Alert.alert(
        'Driver found',
        "We found you a driver. They're on their way to pick up your order.",
      );
    });
    
    // subscribe to driver location change
Once the driver has processed the order, the location can constantly be watched with client-driver-location. It will show the driver’s location to the user.

this.user_ride_channel.bind('client-driver-location', data => {
      // driver location received
      let driverLocation = regionFrom(
        data.latitude,
        data.longitude,
        data.accuracy,
      );
      
      // update the marker of the driver's location
      this.setState({
        driverLocation,
      });
    });
After that, we need to listen to client-order-updates. Here, the step value will be used to show order status. Step 1 is for the driver to accept the request, and step 2 is for the driver to receive the order from the restaurant.

this.user_ride_channel.bind('client-order-update', data => {
      this.setState({
        orderStatusText: orderSteps[data.step],
      });
    });
The render() method.

render() {
      const {driverLocation, orderStatusText} = this.state;
    
      return (
        <View style={styles.wrapper}>
          <View style={styles.infoContainer}>
            <Text style={styles.infoText}>{orderStatusText}</Text>
    
            <Button
              onPress={() => this.contactDriver()}
              title="Contact driver"
              color="#c53c3c"
            />
          </View>
    
          <View style={styles.mapContainer}>
            <MapView
              style={styles.map}
              zoomControlEnabled={true}
              initialRegion={this.customer_location}>
              <MapView.Marker
                coordinate={{
                  latitude: this.customer_location.latitude,
                  longitude: this.customer_location.longitude,
                }}
                title={'Your location'}
              />
    
              {driverLocation && (
                <MapView.Marker
                  coordinate={driverLocation}
                  title={'Driver location'}
                  pinColor={'#6f42c1'}
                />
              )}
    
              <MapView.Marker
                coordinate={{
                  latitude: this.restaurant_location[0],
                  longitude: this.restaurant_location[1],
                }}
                title={'Restaurant location'}
                pinColor={'#4CDB00'}
              />
    
              {driverLocation && (
                <MapViewDirections
                  origin={driverLocation}
                  destination={{
                    latitude: this.restaurant_location[0],
                    longitude: this.restaurant_location[1],
                  }}
                  apikey={GOOGLE_API_KEY}
                  strokeWidth={3}
                  strokeColor="hotpink"
                />
              )}
    
              <MapViewDirections
                origin={{
                  latitude: this.restaurant_location[0],
                  longitude: this.restaurant_location[1],
                }}
                destination={{
                  latitude: this.customer_location.latitude,
                  longitude: this.customer_location.longitude,
                }}
                apikey={GOOGLE_API_KEY}
                strokeWidth={3}
                strokeColor="#1b77fb"
              />
            </MapView>
          </View>
        </View>
      );
    }
Authentication Server
We are now ready to go with the authentication server. Update the server/.emv with your channels app instance credentials.

PUSHER_APP_ID="YOUR PUSHER APP ID"
    PUSHER_APP_KEY="YOUR PUSHER APP KEY"
    PUSHER_APP_SECRET="YOUR PUSHER APP SECRET"
    PUSHER_APP_CLUSTER="YOUR PUSHER APP CLUSTER"
Import the required packages.

// server/index.js
    const express = require('express');
    const bodyParser = require('body-parser');
    const cors = require('cors');
    
    const Pusher = require('pusher');
Initialize Node.js client for channels.

var pusher = new Pusher({
      appId: process.env.PUSHER_APP_ID,
      key: process.env.PUSHER_APP_KEY,
      secret: process.env.PUSHER_APP_SECRET,
      cluster: process.env.PUSHER_APP_CLUSTER,
    });
The next step is to import the food data.

const {foods} = require('./data/foods.js');
Initialize the Express server to authenticate the users. The channel client on the app requests the route when the connection is initialized. The user will be allowed to trigger events directly from the client side. It will immediately authenticate the users.

app.post('/pusher/auth', function(req, res) {
      var socketId = req.body.socket_id;
      var channel = req.body.channel_name;
      var auth = pusher.authenticate(socketId, channel); // authenticate the request
      res.send(auth);
    });
Now, let’s explore the server.

const PORT = 5000;
    app.listen(PORT, err => {
      if (err) {
        console.error(err);
      } else {
        console.log(`Running on ports ${PORT}`);
      }
    });
Run the App
Now, we are all set to run the application. Run the server and expose it with ngrok.

node server/index.js
    ~/Downloads/ngrok http 5000
After that, update the .emv file with the HTTPS URL. With the following, run the application.

react-native run-android
You have successfully developed a food delivery application with Reactjs.

Conclusion
In this article, we have learned to develop a basic food delivery application with ReactJS. We have used various packages for implementing the app, using React Native Maps to show locations for the driver, restaurant, and user and using React Native Maps to indicate the path between two points. Finally, you are all set from food list screens to track order screens and to run your app.
