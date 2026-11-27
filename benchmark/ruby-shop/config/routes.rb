Rails.application.routes.draw do
  get 'up' => 'rails/health#show', as: :rails_health_check

  namespace :api do
    get 'products', to: 'products#index'
    get 'products/search', to: 'products#search'
    get 'products/:id', to: 'products#show'

    get 'images', to: 'images#show'
    get 'thumbs', to: 'images#thumb'

    post 'auth/login', to: 'sessions#create'
    post 'auth/reset', to: 'sessions#reset'
    get 'auth/logout', to: 'sessions#destroy'
    get 'auth/return', to: 'sessions#return_to'

    get 'search', to: 'pages#search'
    get 'search/safe', to: 'pages#safe_search'
    get 'recommendations', to: 'pages#recommendations'

    resource :account, only: [] do
      post :avatar
      post :cart
      post :import
    end

    resources :orders, only: [] do
      get :payment, on: :member
    end

    namespace :admin do
      post 'export', to: 'tools#export'
      get 'report', to: 'tools#report'
      post 'templates/preview', to: 'tools#preview'
    end
  end
end
