// ==================== REACT NATIVE APP ====================

// App.tsx
import React, { useEffect } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Provider } from 'react-redux';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import * as SplashScreen from 'expo-splash-screen';
import * as Notifications from 'expo-notifications';
import { 
  useFonts,
  Inter_400Regular,
  Inter_500Medium,
  Inter_600SemiBold,
  Inter_700Bold 
} from '@expo-google-fonts/inter';

import { store } from './src/store';
import { AuthProvider } from './src/contexts/AuthContext';
import { ThemeProvider } from './src/contexts/ThemeContext';
import { NotificationProvider } from './src/contexts/NotificationContext';

// Screens
import { SplashScreen as CustomSplash } from './src/screens/SplashScreen';
import { OnboardingScreen } from './src/screens/OnboardingScreen';
import { LoginScreen } from './src/screens/auth/LoginScreen';
import { RegisterScreen } from './src/screens/auth/RegisterScreen';
import { HomeScreen } from './src/screens/HomeScreen';
import { CharactersScreen } from './src/screens/CharactersScreen';
import { ChatScreen } from './src/screens/ChatScreen';
import { ProfileScreen } from './src/screens/ProfileScreen';
import { SettingsScreen } from './src/screens/SettingsScreen';

// Navigation
import { TabNavigator } from './src/navigation/TabNavigator';
import { RootStackParamList } from './src/types/navigation';

SplashScreen.preventAutoHideAsync();

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

const Stack = createNativeStackNavigator<RootStackParamList>();
const queryClient = new QueryClient();

export default function App() {
  const [fontsLoaded] = useFonts({
    Inter_400Regular,
    Inter_500Medium,
    Inter_600SemiBold,
    Inter_700Bold,
  });

  useEffect(() => {
    if (fontsLoaded) {
      SplashScreen.hideAsync();
    }
  }, [fontsLoaded]);

  if (!fontsLoaded) {
    return <CustomSplash />;
  }

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <Provider store={store}>
        <QueryClientProvider client={queryClient}>
          <SafeAreaProvider>
            <ThemeProvider>
              <AuthProvider>
                <NotificationProvider>
                  <NavigationContainer>
                    <StatusBar style="auto" />
                    <Stack.Navigator
                      initialRouteName="Splash"
                      screenOptions={{
                        headerShown: false,
                        animation: 'slide_from_right',
                      }}
                    >
                      <Stack.Screen name="Splash" component={CustomSplash} />
                      <Stack.Screen name="Onboarding" component={OnboardingScreen} />
                      <Stack.Screen name="Login" component={LoginScreen} />
                      <Stack.Screen name="Register" component={RegisterScreen} />
                      <Stack.Screen name="Main" component={TabNavigator} />
                      <Stack.Screen 
                        name="Chat" 
                        component={ChatScreen}
                        options={{
                          animation: 'slide_from_bottom',
                          presentation: 'modal',
                        }}
                      />
                      <Stack.Screen name="Settings" component={SettingsScreen} />
                    </Stack.Navigator>
                  </NavigationContainer>
                </NotificationProvider>
              </AuthProvider>
            </ThemeProvider>
          </SafeAreaProvider>
        </QueryClientProvider>
      </Provider>
    </GestureHandlerRootView>
  );
}

// ==================== HOME SCREEN ====================

// src/screens/HomeScreen.tsx
import React, { useCallback, useRef } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  RefreshControl,
  Dimensions,
  FlatList,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { LinearGradient } from 'expo-linear-gradient';
import Animated, {
  FadeInDown,
  FadeInRight,
  useAnimatedStyle,
  useSharedValue,
  withSpring,
} from 'react-native-reanimated';
import { Ionicons } from '@expo/vector-icons';

import { useAuth } from '@/hooks/useAuth';
import { useConversations } from '@/hooks/useConversations';
import { useCharacters } from '@/hooks/useCharacters';
import { ConversationCard } from '@/components/ConversationCard';
import { CharacterCard } from '@/components/CharacterCard';
import { SearchBar } from '@/components/SearchBar';
import { EmptyState } from '@/components/EmptyState';
import { LoadingState } from '@/components/LoadingState';
import { styles } from '@/styles/HomeStyles';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

export function HomeScreen() {
  const navigation = useNavigation();
  const { user } = useAuth();
  const scrollY = useSharedValue(0);
  
  const {
    conversations,
    loading: conversationsLoading,
    refetch: refetchConversations,
  } = useConversations();
  
  const {
    characters,
    loading: charactersLoading,
    refetch: refetchCharacters,
  } = useCharacters({ featured: true });

  const [refreshing, setRefreshing] = React.useState(false);
  const [searchQuery, setSearchQuery] = React.useState('');

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all([
      refetchConversations(),
      refetchCharacters(),
    ]);
    setRefreshing(false);
  }, []);

  const headerAnimatedStyle = useAnimatedStyle(() => {
    return {
      transform: [
        {
          translateY: withSpring(
            scrollY.value > 50 ? -100 : 0,
            { damping: 15 }
          ),
        },
      ],
    };
  });

  const filteredConversations = conversations.filter(conv =>
    conv.character.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    conv.lastMessage?.content.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const renderHeader = () => (
    <Animated.View style={[styles.header, headerAnimatedStyle]}>
      <LinearGradient
        colors={['#667eea', '#764ba2']}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={styles.headerGradient}
      >
        <SafeAreaView edges={['top']}>
          <View style={styles.headerContent}>
            <View>
              <Text style={styles.greeting}>
                Good {getTimeOfDay()}, {user?.displayName || 'Friend'}! 👋
              </Text>
              <Text style={styles.subtitle}>
                Who would you like to chat with today?
              </Text>
            </View>
            <TouchableOpacity
              onPress={() => navigation.navigate('Profile')}
              style={styles.profileButton}
            >
              <Animated.Image
                entering={FadeInRight.delay(300)}
                source={{ uri: user?.avatarUrl || 'https://i.pravatar.cc/150' }}
                style={styles.profileImage}
              />
            </TouchableOpacity>
          </View>
        </SafeAreaView>
      </LinearGradient>
    </Animated.View>
  );

  const renderFeaturedCharacters = () => (
    <Animated.View 
      entering={FadeInDown.delay(200)}
      style={styles.section}
    >
      <View style={styles.sectionHeader}>
        <Text style={styles.sectionTitle}>Featured Characters</Text>
        <TouchableOpacity
          onPress={() => navigation.navigate('Characters')}
          style={styles.seeAllButton}
        >
          <Text style={styles.seeAllText}>See all</Text>
          <Ionicons name="arrow-forward" size={16} color="#667eea" />
        </TouchableOpacity>
      </View>

      {charactersLoading ? (
        <LoadingState />
      ) : (
        <FlatList
          horizontal
          showsHorizontalScrollIndicator={false}
          data={characters}
          keyExtractor={(item) => item.id}
          renderItem={({ item, index }) => (
            <Animated.View
              entering={FadeInRight.delay(index * 100)}
              style={styles.characterCardWrapper}
            >
              <CharacterCard
                character={item}
                onPress={() => navigation.navigate('Chat', { 
                  characterId: item.id,
                  isNewConversation: true 
                })}
              />
            </Animated.View>
          )}
          contentContainerStyle={styles.characterList}
        />
      )}
    </Animated.View>
  );

  const renderRecentConversations = () => (
    <Animated.View 
      entering={FadeInDown.delay(400)}
      style={styles.section}
    >
      <Text style={styles.sectionTitle}>Recent Conversations</Text>
      
      {conversationsLoading ? (
        <LoadingState />
      ) : filteredConversations.length === 0 ? (
        <EmptyState
          icon="chatbubbles-outline"
          title="No conversations yet"
          message="Start chatting with a character to see your conversations here"
          actionLabel="Browse Characters"
          onAction={() => navigation.navigate('Characters')}
        />
      ) : (
        <View style={styles.conversationList}>
          {filteredConversations.slice(0, 5).map((conversation, index) => (
            <Animated.View
              key={conversation.id}
              entering={FadeInDown.delay(500 + index * 100)}
            >
              <ConversationCard
                conversation={conversation}
                onPress={() => navigation.navigate('Chat', {
                  conversationId: conversation.id,
                  characterId: conversation.character.id,
                })}
              />
            </Animated.View>
          ))}
        </View>
      )}
    </Animated.View>
  );

  return (
    <View style={styles.container}>
      {renderHeader()}
      
      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor="#667eea"
          />
        }
        onScroll={(event) => {
          scrollY.value = event.nativeEvent.contentOffset.y;
        }}
        scrollEventThrottle={16}
      >
        <SearchBar
          value={searchQuery}
          onChangeText={setSearchQuery}
          placeholder="Search conversations..."
          style={styles.searchBar}
        />

        {renderFeaturedCharacters()}
        {renderRecentConversations()}

        <View style={styles.bottomSpacing} />
      </ScrollView>
    </View>
  );
}

function getTimeOfDay() {
  const hour = new Date().getHours();
  if (hour < 12) return 'morning';
  if (hour < 18) return 'afternoon';
  return 'evening';
}

// ==================== CHAT SCREEN ====================

// src/screens/ChatScreen.tsx
import React, { useState, useRef, useCallback, useEffect } from 'react';
import {
  View,
  KeyboardAvoidingView,
  Platform,
  FlatList,
  TextInput,
  TouchableOpacity,
  Alert,
  Keyboard,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRoute, useNavigation } from '@react-navigation/native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withSpring,
  FadeIn,
  FadeOut,
  Layout,
} from 'react-native-reanimated';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import * as ImagePicker from 'expo-image-picker';
import Voice from '@react-native-voice/voice';

import { useChat } from '@/hooks/useChat';
import { useWebSocket } from '@/hooks/useWebSocket';
import { ChatHeader } from '@/components/chat/ChatHeader';
import { MessageBubble } from '@/components/chat/MessageBubble';
import { ChatInput } from '@/components/chat/ChatInput';
import { TypingIndicator } from '@/components/chat/TypingIndicator';
import { ChatActions } from '@/components/chat/ChatActions';
import { VoiceRecorder } from '@/components/chat/VoiceRecorder';
import { Message } from '@/types/chat';
import { styles } from '@/styles/ChatStyles';

export function ChatScreen() {
  const navigation = useNavigation();
  const route = useRoute();
  const { conversationId, characterId, isNewConversation } = route.params;
  
  const flatListRef = useRef<FlatList>(null);
  const inputRef = useRef<TextInput>(null);
  const keyboardHeight = useSharedValue(0);
  
  const [message, setMessage] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const [showActions, setShowActions] = useState(false);
  
  const {
    messages,
    loading,
    sending,
    sendMessage,
    sendImage,
    deleteMessage,
    editMessage,
  } = useChat(conversationId || null, characterId);
  
  const { isTyping } = useWebSocket(conversationId);

  useEffect(() => {
    // Set up voice recognition
    Voice.onSpeechResults = onSpeechResults;
    Voice.onSpeechEnd = onSpeechEnd;
    
    return () => {
      Voice.destroy().then(Voice.removeAllListeners);
    };
  }, []);

  useEffect(() => {
    // Set up keyboard listeners
    const keyboardWillShow = Keyboard.addListener(
      Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow',
      (e) => {
        keyboardHeight.value = withSpring(e.endCoordinates.height);
      }
    );
    
    const keyboardWillHide = Keyboard.addListener(
      Platform.OS === 'ios' ? 'keyboardWillHide' : 'keyboardDidHide',
      () => {
        keyboardHeight.value = withSpring(0);
      }
    );
    
    return () => {
      keyboardWillShow.remove();
      keyboardWillHide.remove();
    };
  }, []);

  const onSpeechResults = (e: any) => {
    setMessage(e.value[0]);
  };

  const onSpeechEnd = () => {
    setIsRecording(false);
  };

  const handleSend = useCallback(async () => {
    if (!message.trim() || sending) return;
    
    const text = message.trim();
    setMessage('');
    
    await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    await sendMessage(text);
    
    // Scroll to bottom
    setTimeout(() => {
      flatListRef.current?.scrollToEnd({ animated: true });
    }, 100);
  }, [message, sending, sendMessage]);

  const handleImagePicker = useCallback(async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    
    if (status !== 'granted') {
      Alert.alert(
        'Permission Required',
        'Please enable photo library access to send images.'
      );
      return;
    }
    
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.8,
    });
    
    if (!result.canceled) {
      await sendImage(result.assets[0].uri);
    }
  }, [sendImage]);

  const handleCamera = useCallback(async () => {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    
    if (status !== 'granted') {
      Alert.alert(
        'Permission Required',
        'Please enable camera access to take photos.'
      );
      return;
    }
    
    const result = await ImagePicker.launchCameraAsync({
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.8,
    });
    
    if (!result.canceled) {
      await sendImage(result.assets[0].uri);
    }
  }, [sendImage]);

  const handleVoiceRecord = useCallback(async () => {
    if (isRecording) {
      try {
        await Voice.stop();
        setIsRecording(false);
      } catch (error) {
        console.error('Failed to stop recording:', error);
      }
    } else {
      try {
        await Voice.start('en-US');
        setIsRecording(true);
        await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
      } catch (error) {
        console.error('Failed to start recording:', error);
        Alert.alert(
          'Voice Recognition',
          'Voice recognition is not available on this device.'
        );
      }
    }
  }, [isRecording]);

  const handleMessageLongPress = useCallback((msg: Message) => {
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    
    Alert.alert(
      'Message Options',
      undefined,
      [
        {
          text: 'Copy',
          onPress: () => {
            // Copy to clipboard
            Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
          },
        },
        msg.role === 'user' && {
          text: 'Edit',
          onPress: () => {
            setMessage(msg.content);
            editMessage(msg.id, msg.content);
          },
        },
        msg.role === 'user' && {
          text: 'Delete',
          style: 'destructive',
          onPress: () => deleteMessage(msg.id),
        },
        {
          text: 'Cancel',
          style: 'cancel',
        },
      ].filter(Boolean)
    );
  }, [deleteMessage, editMessage]);

  const renderMessage = useCallback(({ item, index }: { item: Message; index: number }) => (
    <Animated.View
      entering={FadeIn.delay(index * 50)}
      layout={Layout.springify()}
    >
      <MessageBubble
        message={item}
        onLongPress={() => handleMessageLongPress(item)}
        showAvatar={
          index === 0 ||
          messages[index - 1]?.role !== item.role
        }
      />
    </Animated.View>
  ), [messages, handleMessageLongPress]);

  const keyboardAvoidingStyle = useAnimatedStyle(() => ({
    paddingBottom: keyboardHeight.value,
  }));

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <ChatHeader
        character={messages[0]?.character}
        onBack={() => navigation.goBack()}
        onOptions={() => setShowActions(true)}
      />
      
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.keyboardAvoid}
        keyboardVerticalOffset={0}
      >
        <FlatList
          ref={flatListRef}
          data={messages}
          keyExtractor={(item) => item.id}
          renderItem={renderMessage}
          contentContainerStyle={styles.messageList}
          inverted
          onContentSizeChange={() => {
            flatListRef.current?.scrollToEnd({ animated: false });
          }}
          ListEmptyComponent={
            loading ? (
              <View style={styles.loadingContainer}>
                <TypingIndicator />
              </View>
            ) : null
          }
          ListFooterComponent={
            isTyping ? <TypingIndicator /> : null
          }
        />
        
        <Animated.View style={[styles.inputContainer, keyboardAvoidingStyle]}>
          <ChatInput
            ref={inputRef}
            value={message}
            onChangeText={setMessage}
            onSend={handleSend}
            onImagePress={handleImagePicker}
            onCameraPress={handleCamera}
            onVoicePress={handleVoiceRecord}
            sending={sending}
            isRecording={isRecording}
          />
        </Animated.View>
      </KeyboardAvoidingView>
      
      {showActions && (
        <ChatActions
          onClose={() => setShowActions(false)}
          onClearChat={() => {
            Alert.alert(
              'Clear Chat',
              'Are you sure you want to clear this conversation?',
              [
                { text: 'Cancel', style: 'cancel' },
                {
                  text: 'Clear',
                  style: 'destructive',
                  onPress: () => {
                    // Clear chat logic
                    setShowActions(false);
                  },
                },
              ]
            );
          }}
        />
      )}
      
      {isRecording && (
        <VoiceRecorder
          onStop={handleVoiceRecord}
          amplitude={0.5} // Would need real amplitude from Voice
        />
      )}
    </SafeAreaView>
  );
}

// ==================== NAVIGATION ====================

// src/navigation/TabNavigator.tsx
import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import { BlurView } from 'expo-blur';
import { Platform, View } from 'react-native';
import Animated, {
  useAnimatedStyle,
  withSpring,
  interpolate,
} from 'react-native-reanimated';

import { HomeScreen } from '@/screens/HomeScreen';
import { CharactersScreen } from '@/screens/CharactersScreen';
import { ConversationsScreen } from '@/screens/ConversationsScreen';
import { ProfileScreen } from '@/screens/ProfileScreen';
import { useTheme } from '@/hooks/useTheme';

const Tab = createBottomTabNavigator();

const AnimatedIcon = ({ name, focused, color }: any) => {
  const animatedStyle = useAnimatedStyle(() => ({
    transform: [
      {
        scale: withSpring(focused ? 1.2 : 1, {
          damping: 15,
        }),
      },
    ],
  }));

  return (
    <Animated.View style={animatedStyle}>
      <Ionicons name={name} size={24} color={color} />
    </Animated.View>
  );
};

export function TabNavigator() {
  const { theme } = useTheme();

  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          position: 'absolute',
          backgroundColor: Platform.OS === 'ios' ? 'transparent' : theme.colors.card,
          borderTopWidth: 0,
          elevation: 0,
        },
        tabBarBackground: () =>
          Platform.OS === 'ios' ? (
            <BlurView
              tint={theme.dark ? 'dark' : 'light'}
              intensity={80}
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
              }}
            />
          ) : null,
        tabBarActiveTintColor: theme.colors.primary,
        tabBarInactiveTintColor: theme.colors.textSecondary,
      }}
    >
      <Tab.Screen
        name="Home"
        component={HomeScreen}
        options={{
          tabBarIcon: ({ focused, color }) => (
            <AnimatedIcon name="home" focused={focused} color={color} />
          ),
        }}
      />
      <Tab.Screen
        name="Characters"
        component={CharactersScreen}
        options={{
          tabBarIcon: ({ focused, color }) => (
            <AnimatedIcon name="people" focused={focused} color={color} />
          ),
        }}
      />
      <Tab.Screen
        name="Chats"
        component={ConversationsScreen}
        options={{
          tabBarIcon: ({ focused, color }) => (
            <AnimatedIcon name="chatbubbles" focused={focused} color={color} />
          ),
        }}
      />
      <Tab.Screen
        name="Profile"
        component={ProfileScreen}
        options={{
          tabBarIcon: ({ focused, color }) => (
            <AnimatedIcon name="person" focused={focused} color={color} />
          ),
        }}
      />
    </Tab.Navigator>
  );
}

// ==================== COMPONENTS ====================

// src/components/chat/MessageBubble.tsx
import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Image,
  Dimensions,
} from 'react-native';
import Animated, {
  useAnimatedStyle,
  withTiming,
  interpolate,
} from 'react-native-reanimated';
import { format } from 'date-fns';
import { LinearGradient } from 'expo-linear-gradient';

import { Message } from '@/types/chat';
import { useTheme } from '@/hooks/useTheme';
import { styles } from '@/styles/MessageStyles';

const { width: SCREEN_WIDTH } = Dimensions.get('window');
const MAX_BUBBLE_WIDTH = SCREEN_WIDTH * 0.75;

interface MessageBubbleProps {
  message: Message;
  onLongPress?: () => void;
  showAvatar?: boolean;
}

export function MessageBubble({ 
  message, 
  onLongPress,
  showAvatar = true 
}: MessageBubbleProps) {
  const { theme } = useTheme();
  const isUser = message.role === 'user';

  const bubbleAnimatedStyle = useAnimatedStyle(() => ({
    opacity: withTiming(1, { duration: 300 }),
    transform: [
      {
        translateX: withTiming(0, {
          duration: 300,
        }),
      },
    ],
  }));

  const renderContent = () => {
    if (message.type === 'image' && message.imageUrl) {
      return (
        <TouchableOpacity onLongPress={onLongPress} activeOpacity={0.9}>
          <Image
            source={{ uri: message.imageUrl }}
            style={styles.messageImage}
            resizeMode="cover"
          />
        </TouchableOpacity>
      );
    }

    return (
      <TouchableOpacity
        onLongPress={onLongPress}
        activeOpacity={0.8}
        style={[
          styles.bubble,
          isUser ? styles.userBubble : styles.assistantBubble,
          { maxWidth: MAX_BUBBLE_WIDTH },
        ]}
      >
        {isUser ? (
          <LinearGradient
            colors={['#667eea', '#764ba2']}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.gradientBubble}
          >
            <Text style={[styles.messageText, styles.userText]}>
              {message.content}
            </Text>
          </LinearGradient>
        ) : (
          <View style={[styles.plainBubble, { backgroundColor: theme.colors.card }]}>
            <Text style={[styles.messageText, { color: theme.colors.text }]}>
              {message.content}
            </Text>
          </View>
        )}
      </TouchableOpacity>
    );
  };

  return (
    <Animated.View
      style={[
        styles.messageContainer,
        isUser ? styles.userContainer : styles.assistantContainer,
        bubbleAnimatedStyle,
      ]}
    >
      {!isUser && showAvatar && (
        <Image
          source={{ uri: message.character?.avatarUrl || 'https://i.pravatar.cc/150' }}
          style={styles.avatar}
        />
      )}
      
      <View style={styles.contentContainer}>
        {renderContent()}
        
        <Text style={[
          styles.timestamp,
          isUser ? styles.userTimestamp : styles.assistantTimestamp,
        ]}>
          {format(new Date(message.createdAt), 'HH:mm')}
        </Text>
      </View>
    </Animated.View>
  );
}

// src/components/CharacterCard.tsx
import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Image,
  Dimensions,
} from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withSpring,
  interpolate,
} from 'react-native-reanimated';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';

import { Character } from '@/types/character';
import { useTheme } from '@/hooks/useTheme';
import { styles } from '@/styles/CharacterCardStyles';

const { width: SCREEN_WIDTH } = Dimensions.get('window');
const CARD_WIDTH = (SCREEN_WIDTH - 48) / 2;

interface CharacterCardProps {
  character: Character;
  onPress: () => void;
  onLongPress?: () => void;
  style?: any;
}

export function CharacterCard({
  character,
  onPress,
  onLongPress,
  style,
}: CharacterCardProps) {
  const { theme } = useTheme();
  const scale = useSharedValue(1);
  const rotation = useSharedValue(0);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [
      { scale: scale.value },
      { rotateZ: `${rotation.value}deg` },
    ],
  }));

  const handlePressIn = () => {
    scale.value = withSpring(0.95, { damping: 15 });
    rotation.value = withSpring(-2, { damping: 15 });
  };

  const handlePressOut = () => {
    scale.value = withSpring(1, { damping: 15 });
    rotation.value = withSpring(0, { damping: 15 });
  };

  const handlePress = async () => {
    await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    onPress();
  };

  const handleLongPress = async () => {
    await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    onLongPress?.();
  };

  return (
    <Animated.View style={[animatedStyle, style]}>
      <TouchableOpacity
        onPress={handlePress}
        onLongPress={handleLongPress}
        onPressIn={handlePressIn}
        onPressOut={handlePressOut}
        activeOpacity={0.9}
        style={[styles.container, { width: CARD_WIDTH }]}
      >
        <View style={styles.imageContainer}>
          <Image
            source={{ uri: character.avatarUrl }}
            style={styles.image}
            resizeMode="cover"
          />
          <LinearGradient
            colors={['transparent', 'rgba(0,0,0,0.8)']}
            style={styles.gradient}
          />
          
          {character.isOnline && (
            <View style={styles.onlineIndicator}>
              <View style={styles.onlineDot} />
            </View>
          )}
          
          <View style={styles.ratingContainer}>
            <Ionicons name="star" size={14} color="#FFD700" />
            <Text style={styles.rating}>{character.rating.toFixed(1)}</Text>
          </View>
        </View>
        
        <View style={styles.content}>
          <Text style={[styles.name, { color: theme.colors.text }]} numberOfLines={1}>
            {character.name}
          </Text>
          <Text style={[styles.description, { color: theme.colors.textSecondary }]} numberOfLines={2}>
            {character.description}
          </Text>
          
          <View style={styles.tags}>
            {character.tags.slice(0, 2).map((tag, index) => (
              <View key={index} style={[styles.tag, { backgroundColor: theme.colors.border }]}>
                <Text style={[styles.tagText, { color: theme.colors.textSecondary }]}>
                  {tag}
                </Text>
              </View>
            ))}
            {character.tags.length > 2 && (
              <Text style={[styles.moreText, { color: theme.colors.textSecondary }]}>
                +{character.tags.length - 2}
              </Text>
            )}
          </View>
        </View>
      </TouchableOpacity>
    </Animated.View>
  );
}

// ==================== HOOKS ====================

// src/hooks/useChat.ts
import { useState, useEffect, useCallback } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import AsyncStorage from '@react-native-async-storage/async-storage';

import { api } from '@/services/api';
import { Message, SendMessageRequest } from '@/types/chat';
import { useWebSocket } from './useWebSocket';
import { useAuth } from './useAuth';

export function useChat(conversationId: string | null, characterId: string) {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const { sendMessage: wsSendMessage } = useWebSocket(conversationId);
  
  const [messages, setMessages] = useState<Message[]>([]);
  const [sending, setSending] = useState(false);

  // Fetch messages
  const { data: fetchedMessages, isLoading } = useQuery({
    queryKey: ['messages', conversationId],
    queryFn: async () => {
      if (!conversationId) return [];
      const response = await api.get(`/conversations/${conversationId}/messages`);
      return response.data;
    },
    enabled: !!conversationId,
  });

  // Create conversation mutation
  const createConversationMutation = useMutation({
    mutationFn: async (message: string) => {
      const response = await api.post('/conversations', {
        characterId,
        initialMessage: message,
      });
      return response.data;
    },
  });

  // Send message mutation
  const sendMessageMutation = useMutation({
    mutationFn: async ({ conversationId, message }: { conversationId: string; message: string }) => {
      const response = await api.post(`/conversations/${conversationId}/messages`, {
        content: message,
        type: 'text',
      });
      return response.data;
    },
  });

  useEffect(() => {
    if (fetchedMessages) {
      setMessages(fetchedMessages);
    }
  }, [fetchedMessages]);

  const sendMessage = useCallback(async (content: string) => {
    setSending(true);
    
    try {
      let currentConversationId = conversationId;
      
      // Create conversation if needed
      if (!currentConversationId) {
        const conversation = await createConversationMutation.mutateAsync(content);
        currentConversationId = conversation.id;
        
        // Update local state with new conversation ID
        queryClient.setQueryData(['currentConversation'], conversation);
      } else {
        // Send message
        const message = await sendMessageMutation.mutateAsync({
          conversationId: currentConversationId,
          message: content,
        });
        
        // Optimistically update UI
        setMessages(prev => [...prev, message]);
        
        // Send via WebSocket for real-time updates
        wsSendMessage({
          type: 'message',
          content,
          conversationId: currentConversationId,
        });
      }
      
      // Cache conversation locally
      await AsyncStorage.setItem(
        `conversation:${currentConversationId}`,
        JSON.stringify({ characterId, lastMessage: content })
      );
    } catch (error) {
      console.error('Failed to send message:', error);
      // Show error toast
    } finally {
      setSending(false);
    }
  }, [conversationId, characterId, wsSendMessage]);

  const sendImage = useCallback(async (imageUri: string) => {
    setSending(true);
    
    try {
      const formData = new FormData();
      formData.append('image', {
        uri: imageUri,
        type: 'image/jpeg',
        name: 'photo.jpg',
      } as any);
      
      const response = await api.post(
        `/conversations/${conversationId}/messages/image`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        }
      );
      
      setMessages(prev => [...prev, response.data]);
    } catch (error) {
      console.error('Failed to send image:', error);
    } finally {
      setSending(false);
    }
  }, [conversationId]);

  const deleteMessage = useCallback(async (messageId: string) => {
    try {
      await api.delete(`/messages/${messageId}`);
      setMessages(prev => prev.filter(m => m.id !== messageId));
      queryClient.invalidateQueries(['messages', conversationId]);
    } catch (error) {
      console.error('Failed to delete message:', error);
    }
  }, [conversationId, queryClient]);

  const editMessage = useCallback(async (messageId: string, newContent: string) => {
    try {
      const response = await api.patch(`/messages/${messageId}`, {
        content: newContent,
      });
      
      setMessages(prev =>
        prev.map(m => (m.id === messageId ? response.data : m))
      );
    } catch (error) {
      console.error('Failed to edit message:', error);
    }
  }, []);

  return {
    messages,
    loading: isLoading,
    sending,
    sendMessage,
    sendImage,
    deleteMessage,
    editMessage,
  };
}

// ==================== PUSH NOTIFICATIONS ====================

// src/services/notifications/PushNotificationService.ts
import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';
import Constants from 'expo-constants';
import { Platform } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

import { api } from '@/services/api';

export class PushNotificationService {
  private static instance: PushNotificationService;
  
  static getInstance(): PushNotificationService {
    if (!PushNotificationService.instance) {
      PushNotificationService.instance = new PushNotificationService();
    }
    return PushNotificationService.instance;
  }

  async initialize() {
    // Configure notifications
    Notifications.setNotificationHandler({
      handleNotification: async () => ({
        shouldShowAlert: true,
        shouldPlaySound: true,
        shouldSetBadge: true,
      }),
    });

    // Get permission
    const token = await this.registerForPushNotifications();
    if (token) {
      await this.saveTokenToServer(token);
    }

    // Set up listeners
    this.setupListeners();
  }

  private async registerForPushNotifications(): Promise<string | null> {
    if (!Device.isDevice) {
      console.log('Push notifications only work on physical devices');
      return null;
    }

    const { status: existingStatus } = await Notifications.getPermissionsAsync();
    let finalStatus = existingStatus;

    if (existingStatus !== 'granted') {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }

    if (finalStatus !== 'granted') {
      console.log('Failed to get push token for notifications');
      return null;
    }

    const projectId = Constants.expoConfig?.extra?.eas?.projectId;
    const token = (await Notifications.getExpoPushTokenAsync({ projectId })).data;
    
    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync('default', {
        name: 'default',
        importance: Notifications.AndroidImportance.MAX,
        vibrationPattern: [0, 250, 250, 250],
        lightColor: '#667eea',
      });
    }

    return token;
  }

  private async saveTokenToServer(token: string) {
    try {
      const savedToken = await AsyncStorage.getItem('pushToken');
      
      if (savedToken !== token) {
        await api.post('/users/push-token', {
          token,
          platform: Platform.OS,
          deviceName: Device.deviceName,
        });
        
        await AsyncStorage.setItem('pushToken', token);
      }
    } catch (error) {
      console.error('Failed to save push token:', error);
    }
  }

  private setupListeners() {
    // Handle notification received while app is in foreground
    Notifications.addNotificationReceivedListener(notification => {
      console.log('Notification received:', notification);
    });

    // Handle notification response (user tapped on notification)
    Notifications.addNotificationResponseReceivedListener(response => {
      const data = response.notification.request.content.data;
      
      if (data.type === 'message') {
        // Navigate to chat
        this.navigateToChat(data.conversationId, data.characterId);
      } else if (data.type === 'character_update') {
        // Navigate to character profile
        this.navigateToCharacter(data.characterId);
      }
    });
  }

  async scheduleLocalNotification(
    title: string,
    body: string,
    data?: any,
    trigger?: Notifications.NotificationTriggerInput
  ) {
    await Notifications.scheduleNotificationAsync({
      content: {
        title,
        body,
        data,
        sound: true,
        priority: Notifications.AndroidNotificationPriority.HIGH,
      },
      trigger: trigger || null,
    });
  }

  async cancelAllNotifications() {
    await Notifications.cancelAllScheduledNotificationsAsync();
  }

  async setBadgeCount(count: number) {
    if (Platform.OS === 'ios') {
      await Notifications.setBadgeCountAsync(count);
    }
  }

  private navigateToChat(conversationId: string, characterId: string) {
    // This would be handled by navigation context
    // NavigationService.navigate('Chat', { conversationId, characterId });
  }

  private navigateToCharacter(characterId: string) {
    // NavigationService.navigate('Character', { characterId });
  }
}

// Initialize on app start
export const pushNotifications = PushNotificationService.getInstance();